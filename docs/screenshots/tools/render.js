// Regenerate: node docs/screenshots/tools/render.js docs/screenshots   (needs Playwright + Chromium)
// Renders preview images of VeniceX3 screens from the app's layout values (activity_chat.xml,
// activity_model_picker.xml, colors.xml, strings.xml). One eye = 640x480 at 1dp = 1px.
const { chromium } = require('playwright');
const out = process.argv[2];

const C = { text:'#F2F2F2', muted:'#9AA0A6', accent:'#4FD1C5', red:'#FF6B6B', focus:'#1F3A38',
  panel:'rgba(18,28,28,0.90)', stroke:'rgba(79,209,197,0.33)', pill:'#103A36', mature:'#FF8A65', name:'#FFD180' };

// Placeholder avatar (no real character art is shipped): a simple copper-haired figure.
const avatar = (w, h, full) => `<svg viewBox="0 0 100 ${full?220:100}" width="${w}" height="${h}" preserveAspectRatio="xMidYMid meet">
 <defs><linearGradient id="hair" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FF9A3D"/><stop offset="1" stop-color="#C4501A"/></linearGradient>
 <linearGradient id="dress" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFF3E6"/><stop offset="1" stop-color="#F08A3A"/></linearGradient></defs>
 <path d="M18 ${full?120:98} C10 50 22 12 50 12 C78 12 90 50 82 ${full?120:98} Z" fill="url(#hair)"/>
 ${full?'<path d="M30 70 L70 70 L88 205 L12 205 Z" fill="url(#dress)"/><path d="M40 60 L60 60 L62 78 L38 78 Z" fill="#F7D9C4"/>':'<path d="M28 86 C30 74 70 74 72 86 L76 100 L24 100 Z" fill="url(#dress)"/>'}
 <circle cx="50" cy="46" r="21" fill="#F7D9C4"/>
 <path d="M28 44 C28 22 72 22 72 44 C64 32 40 30 28 44 Z" fill="url(#hair)"/>
 <circle cx="42" cy="49" r="2.6" fill="#7A3B12"/><circle cx="58" cy="49" r="2.6" fill="#7A3B12"/>
 <path d="M44 58 Q50 62 56 58" stroke="#C0604A" stroke-width="1.6" fill="none"/></svg>`;

const wave = () => { let b=''; for (let i=0;i<32;i++){ const l=0.15+0.85*Math.abs(Math.sin(i*0.9)*Math.cos(i*0.37)); b+=`<i style="height:${Math.max(5,l*25)}px"></i>`; } return `<div class="wave">${b}</div>`; };

const css = `*{box-sizing:border-box;margin:0}body{background:#000;font-family:Inter,sans-serif;-webkit-font-smoothing:antialiased}
.eye{width:640px;height:480px;background:#000;padding:16px 24px;display:flex;overflow:hidden}
.v{flex-direction:column}.h{flex-direction:row}
.panel{background:${C.panel};border:1px solid ${C.stroke};border-radius:18px;padding:12px 16px 10px;display:flex;flex-direction:column;min-height:0}
.head{display:flex;align-items:center;gap:8px}
.pill{display:flex;align-items:center;background:${C.pill};border-radius:999px;padding:2px 10px 2px 8px;font-size:13px}
.dot{width:8px;height:8px;border-radius:50%;margin-right:6px}
.name{flex:1;color:${C.name};font-weight:700;font-size:14px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.badge{border:1px solid ${C.mature};color:${C.mature};border-radius:6px;padding:0 5px;font-size:11px}
.model{color:${C.muted};font-size:11px}
.q{color:${C.muted};font-size:15px;margin-top:8px}
.reply{color:${C.text};font-size:19px;font-weight:700;line-height:1.12;margin-top:6px;flex:1;overflow:hidden}
.hint{color:${C.muted};font-size:11px;margin-top:4px}
.wave{display:flex;align-items:center;justify-content:space-between;height:26px;margin-top:6px}
.wave i{display:block;width:8px;border-radius:4px;background:${C.accent}}
.list{flex-direction:column;padding:28px 36px 20px}
.title{color:${C.accent};font-size:16px}.rows{flex:1;margin-top:10px}
.row{color:${C.text};font-size:18px;padding:5px 10px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.row.f{color:${C.accent};background:${C.focus}}.row.s{font-size:13px;padding:3px 6px;white-space:normal}
.row.hd{color:${C.accent};font-size:15px}.row.w{color:${C.mature}}
.lhint{color:${C.muted};font-size:12px;text-align:center}
.pair{display:flex;gap:0}`;

const head = (status, color, name, mature, model='venice-uncensored-1-2') => `<div class="head">
  <div class="pill" style="color:${color}"><span class="dot" style="background:${color}"></span>${status}</div>
  <div class="name">${name}</div>${mature?'<span class="badge">18+</span>':''}<span class="model">${model}</span></div>`;

const chat = ({layout='compact', av=true, status='Ready', color=C.muted, name='Mira', mature=false, q='', listening=false, reply='', hint=''}) => {
  const portrait = layout==='portrait';
  const a = !av ? '' : portrait
    ? `<div style="flex:1;margin-right:12px;display:flex;align-items:center;justify-content:center">${avatar(260,448,true)}</div>`
    : `<div style="width:96px;height:96px;margin-left:12px;margin-bottom:-6px;flex:none">${avatar(96,96,false)}</div>`;
  return `<div class="eye ${portrait?'h':'v'}">${a}<div class="panel" style="${portrait?'flex:1.15':'flex:1;width:100%'}">
    ${head(status,color,name,mature)}
    ${q && !listening ? `<div class="q">“${q}”</div>`:''}
    ${listening ? wave() : ''}
    <div class="reply">${listening ? '' : reply}</div>
    ${hint?`<div class="hint">${hint}</div>`:''}</div></div>`;
};

const list = (title, rows, hint) => `<div class="eye list"><div class="title">${title}</div><div class="rows">${
  rows.map(r=>`<div class="row ${r.c||''}">${r.t}</div>`).join('')}</div><div class="lhint">${hint}</div></div>`;

const ans = 'No rain today. Sunny until about 4, then cloudy and 15° by 6 pm. A light jacket is enough.';
const shots = {
  'chat-listening': chat({status:'Listening', color:C.accent, listening:true, hint:'Tap: done · Double-tap: cancel'}),
  'chat-answer': chat({status:'Ready', q:'Will it rain before my 6 pm dinner?', reply:ans, hint:'Tap: ask again · Double-tap: close · Swipe: scroll'}),
  'chat-greeting': chat({reply:'There you are! I missed you. How’s your day going so far?', hint:'Tap: reply · Long press: new chat · Triple tap: menu'}),
  'chat-portrait': chat({layout:'portrait', mature:true, q:'Will it rain before my 6 pm dinner?', reply:ans, hint:'Tap: ask again · Double-tap: close · Swipe: scroll'}),
  'chat-assistant': chat({av:false, name:'Assistant', reply:'Tap: speak · Long press: new chat · Triple tap: menu'}),
  'menu': list('Menu', [
    {t:'Character: Mira'},{t:'Card stores (get characters)'},{t:'Layout: compact'},{t:'Mature mode (18+): ON', c:'f'},
    {t:'  · Mature text: ON'},{t:'  · Mature images: OFF'}], 'Swipe: move · Tap: select · Double tap: back'),
  'characters': list('Characters', [
    {t:'   Assistant'},{t:'● Mira', c:'f'},{t:'   Rin'},{t:'   Kai  18+'}], 'Swipe: move · Tap: chat · Double tap: back'),
  'card-stores': list('Character card stores', [
    {t:'Import: adb … ConfigActivity --es import_url &lt;link&gt;', c:'s hd'},
    {t:'NSFW cards only show with mature text on. Mature cards involving minors are refused.', c:'s w'},
    {t:'Chub.ai  chub.ai', c:'s hd'},{t:'SFW/NSFW filters; lightly moderated', c:'s'},
    {t:'Paste the card page link (chub.ai/characters/author/name).', c:'s'},
    {t:'Character Tavern  character-tavern.com', c:'s hd'},{t:'Curated, includes adult scenarios', c:'s'},
    {t:'Paste the card page link (character-tavern.com/character/author/name).', c:'s'}], 'Swipe: scroll · Double tap: back · See README'),
  'credits': list('Credits &amp; licences', [
    {t:'VeniceX3 is built with', c:'s hd'},{t:'RayNeo Mercury Android SDK 0.2.6: not stated', c:'s w'},
    {t:'AndroidX (core, appcompat, activity, fragment, recyclerview, lifecycle): Apache-2.0', c:'s'},
    {t:'Kotlin standard library: Apache-2.0', c:'s'},{t:'kotlinx.coroutines: Apache-2.0', c:'s'},
    {t:'OkHttp 4.12 and Okio: Apache-2.0', c:'s'},{t:'JUnit 4.13.2 (tests only): EPL-1.0', c:'s'},
    {t:'JSON-java 20240303 (tests only): Public Domain', c:'s'}], 'Swipe: scroll · Double tap: back · Full list: COMMUNITY.md'),
};

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport:{width:640,height:480}, deviceScaleFactor:2 });
  for (const [n, html] of Object.entries(shots)) {
    await page.setContent(`<style>${css}</style>${html}`); await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path:`${out}/${n}.png`, clip:{x:0,y:0,width:640,height:480} });
  }
  // Both eyes, as BaseMirrorActivity shows them on the glasses.
  await page.setViewportSize({width:1280,height:480});
  await page.setContent(`<style>${css}</style><div class="pair">${shots['chat-answer']}${shots['chat-answer']}</div>`);
  await page.screenshot({ path:`${out}/both-eyes.png`, clip:{x:0,y:0,width:1280,height:480} });
  await browser.close();
})();
