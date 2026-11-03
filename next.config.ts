import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Skill files are read from disk at runtime; keep them out of the client bundle.
  serverExternalPackages: ["gray-matter"],
};

export default nextConfig;
