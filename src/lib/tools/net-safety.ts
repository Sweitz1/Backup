import dns from "node:dns";
import net from "node:net";

/**
 * Addresses web_fetch must never reach: this machine (where Ollama and the app run),
 * the local network, link-local/cloud metadata, multicast and reserved ranges.
 */
// Separate lists: Node's BlockList matches IPv4 lookups against IPv6 ::ffff: rules,
// which would block every IPv4 address.
const blockedV4 = new net.BlockList();
const blockedV6 = new net.BlockList();
for (const [addr, prefix] of [
  ["0.0.0.0", 8],
  ["10.0.0.0", 8],
  ["100.64.0.0", 10],
  ["127.0.0.0", 8],
  ["169.254.0.0", 16],
  ["172.16.0.0", 12],
  ["192.0.0.0", 24],
  ["192.0.2.0", 24],
  ["192.168.0.0", 16],
  ["198.18.0.0", 15],
  ["198.51.100.0", 24],
  ["203.0.113.0", 24],
  ["224.0.0.0", 4],
  ["240.0.0.0", 4],
] as const) {
  blockedV4.addSubnet(addr, prefix, "ipv4");
}
for (const [addr, prefix] of [
  ["::", 128],
  ["::1", 128],
  ["64:ff9b::", 96], // NAT64
  ["100::", 64],
  ["2001:db8::", 32],
  ["2002::", 16], // 6to4
  ["fc00::", 7], // unique local
  ["fe80::", 10], // link-local
  ["ff00::", 8], // multicast
] as const) {
  blockedV6.addSubnet(addr, prefix, "ipv6");
}

export function isBlockedAddress(address: string): boolean {
  const family = net.isIP(address);
  if (family === 4) return blockedV4.check(address, "ipv4");
  if (family === 6) {
    const lower = address.toLowerCase();
    if (lower.startsWith("::ffff:")) {
      // IPv4-mapped: judge by the embedded IPv4 address (dotted or hex form).
      const rest = lower.slice(7);
      if (net.isIP(rest) === 4) return isBlockedAddress(rest);
      const hex = rest.match(/^([0-9a-f]{1,4}):([0-9a-f]{1,4})$/);
      if (!hex) return true;
      const hi = parseInt(hex[1], 16);
      const lo = parseInt(hex[2], 16);
      return isBlockedAddress(`${hi >> 8}.${hi & 255}.${lo >> 8}.${lo & 255}`);
    }
    return blockedV6.check(address, "ipv6");
  }
  return true; // not an IP at all
}

/** Throws unless the URL is plain http(s) to a host that isn't an internal IP literal. */
export function assertPublicUrl(url: URL): void {
  if (url.protocol !== "http:" && url.protocol !== "https:") {
    throw new Error("Only http and https URLs are allowed");
  }
  if (url.username || url.password) throw new Error("URLs with credentials are not allowed");
  const host = url.hostname.replace(/^\[|\]$/g, "");
  if (!host || host === "localhost" || host.endsWith(".localhost")) {
    throw new Error("Local addresses are not allowed");
  }
  if (net.isIP(host) && isBlockedAddress(host)) throw new Error("Local or private addresses are not allowed");
}

type LookupCallback = (
  err: NodeJS.ErrnoException | null,
  address: string | dns.LookupAddress[],
  family?: number,
) => void;

/**
 * DNS lookup for http(s).request that refuses internal addresses. Checking at connect
 * time (not before the request) closes the DNS-rebinding gap.
 */
export function safeLookup(hostname: string, options: dns.LookupOptions, callback: LookupCallback): void {
  dns.lookup(hostname, { ...options, all: true }, (err, addresses) => {
    if (err) return callback(err, "");
    const list = addresses as dns.LookupAddress[];
    if (list.length === 0) return callback(new Error(`No address for ${hostname}`), "");
    const bad = list.find((a) => isBlockedAddress(a.address));
    if (bad) return callback(new Error(`${hostname} resolves to a local or private address`), "");
    if (options.all) return callback(null, list);
    callback(null, list[0].address, list[0].family);
  });
}
