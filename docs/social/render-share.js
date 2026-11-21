// Regenerate: node docs/social/render-share.js docs/screenshots docs/social   (needs Playwright + Chromium)
const { chromium } = require('playwright'); const fs = require('fs');
const [S, D] = process.argv.slice(2);
const img = n => 'data:image/png;base64,' + fs.readFileSync(`${S}/${n}.png`).toString('base64');
const css = `*{margin:0;box-sizing:border-box}body{font-family:Inter,sans-serif;color:#F2F2F2;-webkit-font-smoothing:antialiased}
.c{background:radial-gradient(circle at 85% 10%,#123b37 0,#05100f 55%,#000 100%);position:relative;overflow:hidden}
h1{font-size:64px;font-weight:800;letter-spacing:-1.5px}h1 span{color:#4FD1C5}
.sub{font-size:28px;color:#C9D1D3;margin-top:10px;line-height:1.25}
.f{display:flex;flex-direction:column;gap:12px;margin-top:26px;font-size:24px}
.f div{display:flex;gap:12px;align-items:center}.f b{color:#FFD180;font-weight:700}
.shot{border-radius:16px;border:1px solid rgba(79,209,197,.35);box-shadow:0 20px 60px rgba(0,0,0,.6)}
.foot{position:absolute;left:64px;bottom:40px;font-size:20px;color:#9AA0A6}.foot b{color:#4FD1C5}
.tag{display:inline-block;border:1px solid #FF8A65;color:#FF8A65;border-radius:8px;padding:2px 10px;font-size:18px;margin-top:22px}`;
const features = `<div class="f">
 <div>🎭 <span><b>SillyTavern cards</b>: Chub, Character Tavern & more</span></div>
 <div>🎙️ <span><b>Tap & talk</b>: answers in both lenses</span></div>
 <div>🎨 <span><b>"Send me a selfie"</b>: AI-drawn pictures</span></div>
 <div>🔒 <span><b>Private</b>: chats stay in memory</span></div></div>`;
const wide = `<div class="c" style="width:1200px;height:630px;padding:56px 64px;display:flex;gap:40px">
 <div style="flex:1"><h1>Venice<span>X3</span></h1><div class="sub">AI characters on your<br>RayNeo X3 Pro glasses</div>${features}
 <div class="tag">Optional 18+ mode · off by default</div></div>
 <div style="width:520px;display:flex;flex-direction:column;gap:18px;justify-content:center">
  <img class="shot" src="${img('chat-portrait')}" style="width:520px">
 </div><div class="foot">Download v0.2.0 · <b>github.com/Sweitz1/Backup/releases</b></div></div>`;
const square = `<div class="c" style="width:1080px;height:1080px;padding:64px">
 <h1 style="font-size:76px">Venice<span>X3</span></h1><div class="sub" style="font-size:32px">AI characters on your RayNeo X3 Pro glasses</div>
 <div style="display:flex;gap:20px;margin-top:36px"><img class="shot" src="${img('chat-listening')}" style="width:466px"><img class="shot" src="${img('chat-portrait')}" style="width:466px"></div>
 ${features.replace('class="f"','class="f" style="font-size:27px;margin-top:36px"')}
 <div class="foot" style="left:64px;bottom:48px;font-size:22px">Download v0.2.0 · <b>github.com/Sweitz1/Backup/releases</b> · Optional 18+ mode, off by default</div></div>`;
(async () => {
  const b = await chromium.launch(); const p = await b.newPage({ deviceScaleFactor: 2, viewport:{width:1200,height:630} });
  await p.setContent(`<style>${css}</style>${wide}`); await p.evaluate(()=>document.fonts.ready);
  await p.screenshot({ path:`${D}/share-wide.png`, clip:{x:0,y:0,width:1200,height:630} });
  await p.setViewportSize({width:1080,height:1080});
  await p.setContent(`<style>${css}</style>${square}`); await p.evaluate(()=>document.fonts.ready);
  await p.screenshot({ path:`${D}/share-square.png`, clip:{x:0,y:0,width:1080,height:1080} });
  await b.close();
})();
