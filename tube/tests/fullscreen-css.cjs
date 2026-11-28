const {chromium}=require(process.env.MM_PLAYWRIGHT_MODULE||'playwright-core');
const fs=require('fs'),path=require('path'),assert=require('node:assert/strict');
(async()=>{const browser=await chromium.launch({channel:'msedge',headless:true});try{
 const page=await browser.newPage({viewport:{width:800,height:400}});
 await page.setContent(`<style>body{margin:0}#player-container-id{position:relative;width:800px;height:400px}video{width:800px;height:400px}.portal{position:fixed;inset:0;z-index:5}.menu{position:fixed;left:200px;top:100px;width:400px;height:200px;background:white}bottom-sheet-container{display:none}#unrelated{position:fixed;top:0}</style><div id="player-container-id"><video></video></div><ytm-app><span id="unrelated">Recommendation</span><ytm-bottom-sheet-renderer id="recommendation-button">More</ytm-bottom-sheet-renderer><bottom-sheet-container class="portal"><button class="menu">Quality</button></bottom-sheet-container></ytm-app>`);
 await page.evaluate(()=>{const v=document.querySelector('video');Object.defineProperty(v,'readyState',{value:4});window.__magdiCarLease='test';window.__magdiCarDocument='doc';window.__magdiArtwork=()=>({id:'test-video'});});
 for(const asset of ['video_fit.js','car_controller.js'])await page.addScriptTag({content:fs.readFileSync(path.join(__dirname,'../src/main/assets',asset),'utf8')});
 for(let pass=0;pass<3;pass++){
  assert.equal(await page.evaluate(()=>window.__magdiCar.fullscreen(true,'test','doc','test-video',1)),true);
  for(const fill of [false,true,false]){
   await page.evaluate(fill=>{window.MMVideoFit.set(fill);document.querySelector('bottom-sheet-container').style.display='block';},fill);
   assert.equal(await page.locator('.menu').evaluate(e=>getComputedStyle(e).visibility),'visible');
   await page.locator('.menu').click({timeout:2000});
   assert.equal(await page.locator('#unrelated').evaluate(e=>getComputedStyle(e).visibility),'hidden');
   assert.equal(await page.locator('#recommendation-button').evaluate(e=>getComputedStyle(e).visibility),'hidden');
   await page.evaluate(()=>document.querySelector('bottom-sheet-container').style.display='none');
   assert.equal(await page.locator('.menu').isVisible(),false,'closed menus stay closed');
  }
  await page.evaluate(()=>window.__magdiCar.exitFullscreen());
  assert.equal(await page.locator('#unrelated').evaluate(e=>getComputedStyle(e).visibility),'visible');
 }
 await page.evaluate(()=>{const b=document.createElement('button');b.className='fullscreen-icon';b.textContent='Fullscreen';b.style.cssText='position:absolute;top:10px;right:10px;z-index:2000';window.nativeEntries=0;b.onclick=()=>nativeEntries++;document.getElementById('player-container-id').append(b);window.__magdiCar.fullscreen(true,'test','doc','test-video',2);});
 await page.locator('.fullscreen-icon').click();assert.equal(await page.evaluate(()=>nativeEntries),0);assert.equal(await page.evaluate(()=>window.MMVideoFit.state().fullscreen),false);assert.equal(await page.evaluate(()=>window.__magdiCar.state().inlineExit),true);
 console.log('PASS: YouTube button exits inline fullscreen without invoking a second enter.');
 console.log('PASS: real CSS cascade; modern menu receives clicks over fullscreen player through repeated Full/Fill/exit cycles; unrelated and closed menus stay hidden');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
