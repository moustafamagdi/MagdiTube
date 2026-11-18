// Real Android WebViews; launch debug ControllerHarnessActivity and forward CDP to 9226.
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {writeFileSync} from 'node:fs';
const pages=await(await fetch('http://127.0.0.1:9226/json/list')).json();
const sockets=[],checks=[];
async function client(title){const page=pages.find(p=>p.title===title);assert(page,`Missing ${title}`);const ws=new WebSocket(page.webSocketDebuggerUrl);sockets.push(ws);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});let serial=0;const pending=new Map();ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(m.error):p.resolve(m.result);}};return async expression=>{const id=++serial;const result=await new Promise((resolve,reject)=>{pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method:'Runtime.evaluate',params:{expression:`(async()=>{${expression}})()`,awaitPromise:true,returnByValue:true,userGesture:false}}));});if(result.exceptionDetails)throw Error(result.exceptionDetails.exception?.description||result.exceptionDetails.text);return result.result.value;};}
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(run,expression){for(let i=0;i<100;i++){try{if(await run('return '+expression))return;}catch{}await sleep(150);}throw Error('Timeout: '+expression);}
function pass(name){checks.push(name);console.log('PASS '+name);}
try{
 const phone=await client('Phone Controller'),car=await client('Car Fixture');
 await until(phone,'JSON.parse(Harness.state()).connected&&window.__magdiPhoneEpoch>=0');
 await phone("document.querySelector('a').click()");await until(phone,"JSON.parse(Harness.state()).phase==='playing'");
 const native=await car("try{await document.querySelector('video').requestFullscreen();return 'entered'}catch(e){return 'rejected'}");
 assert.equal(native,'rejected');pass('Native fullscreen is rejected without a gesture in the car document; no gesture is fabricated');
 await car(`const p=document.querySelector('#player'),v=document.querySelector('video');
 p.id='player-container-id';p.style.cssText='position:relative;transform:translateZ(0);overflow:hidden;contain:paint;width:180px;height:100px';
 const inner=document.createElement('div');inner.className='html5-video-player';inner.style.cssText='width:180px;height:100px';p.append(inner);inner.append(v);
 v.controls=false;v.style.cssText='width:160px;height:90px;left:5px;top:3px';
 const controls=document.createElement('div');controls.id='fixture-controls';controls.style.cssText='position:absolute;bottom:10px;left:10px;right:10px;height:48px;background:#234;z-index:20;visibility:hidden';
 const play=document.createElement('button');play.id='fixture-play';play.textContent='Pause / Play';play.style.cssText='height:48px;width:120px';play.onclick=()=>v.paused?v.play():v.pause();controls.append(play);
 const seek=document.createElement('button');seek.id='fixture-seek';seek.textContent='+5s';seek.style.cssText='height:48px;width:60px';seek.onclick=()=>v.currentTime+=5;controls.append(seek);p.append(controls);
 p.addEventListener('pointerdown',()=>controls.style.visibility=controls.style.visibility==='hidden'?'visible':'hidden');
 const other=document.createElement('div');other.id='movie_player';other.style.cssText='background:black;width:10px;height:10px';document.body.append(other);
 window.originalPlayer=p;window.originalVideo=v;window.originalParent=v.parentNode;window.originalDocument=__magdiCarDocument;window.originalStyle=p.getAttribute('style');`);
 await phone("Harness.command('fullscreen',0)");await until(car,"!!document.getElementById('magdi-car-fullscreen')");await sleep(600);
 await car("originalPlayer.dispatchEvent(new Event('pointerdown'))");
 assert.equal(await car("return getComputedStyle(document.querySelector('#fixture-play')).visibility"),'visible','YouTube-style controls must remain visible when revealed');
 const hits=await car(`return ['fixture-play','fixture-seek'].every(id=>{const b=document.getElementById(id),r=b.getBoundingClientRect();return r.width>0&&r.height>0&&r.left>=0&&r.bottom<=innerHeight&&document.elementFromPoint(r.left+r.width/2,r.top+r.height/2)===b})`);
 assert.ok(hits,'Controls must receive input above the video, not merely exist in the DOM');
 if(process.env.MM_TEST_ADB)writeFileSync('dist/player-fullscreen-controls.png',execFileSync(process.env.MM_TEST_ADB,['-s','emulator-5560','exec-out','screencap','-p']));
 pass('The active player retains visible, hittable controls outside the inner video wrapper');
 const geometry=await car(`const r=originalVideo.getBoundingClientRect();return {w:r.width,h:r.height,vw:innerWidth,vh:innerHeight,top:document.elementFromPoint(innerWidth/2,innerHeight/2)===originalVideo}`);
 assert.ok(Math.abs(geometry.w-geometry.vw)<2&&Math.abs(geometry.h-geometry.vh)<2&&geometry.top);pass('Video fills the viewport without unrelated black containers covering it');
 await car("document.querySelector('#fixture-play').click()");await until(phone,"JSON.parse(Harness.state()).phase==='paused'&&JSON.parse(Harness.controls()).Play");
 await car("document.querySelector('#fixture-play').click()");await until(phone,"JSON.parse(Harness.state()).phase==='playing'&&JSON.parse(Harness.controls()).Pause");
 await car("originalVideo.pause();originalVideo.currentTime=5;document.querySelector('#fixture-seek').click()");assert.equal(await car('return originalVideo.currentTime'),10);await car('await originalVideo.play()');
 pass('Car page Play/Pause and seek operate the same video and update native phone controls');
 await car("originalPlayer.dispatchEvent(new Event('pointerdown'))");assert.equal(await car("return getComputedStyle(document.querySelector('#fixture-play')).visibility"),'hidden');
 await car("originalPlayer.dispatchEvent(new Event('pointerdown'))");pass('Player controls retain their own show/hide behavior');
 await phone('Harness.coverCar(true)');await sleep(1400);await phone('Harness.coverCar(false)');await sleep(500);
 assert.equal(await car("return !originalVideo.paused&&originalVideo.parentNode===originalParent&&__magdiCarDocument===originalDocument&&getComputedStyle(document.querySelector('#fixture-play')).visibility==='visible'"),true);pass('Cover/return preserves playback, document and player controls');
 await phone("Harness.command('fullscreen',0)");await until(car,"!document.getElementById('magdi-car-fullscreen')");
 assert.equal(await car("return originalPlayer.getAttribute('style')===originalStyle&&!originalVideo.controls&&!document.querySelector('[data-mm-fullscreen]')"),true);pass('Exit restores page layout and original video controls setting');
 await car("originalPlayer.removeAttribute('id');const inner=originalVideo.parentNode;inner.id='movie_player';inner.append(document.querySelector('#fixture-controls'))");
 await phone("Harness.command('fullscreen',0)");await until(car,"!!document.getElementById('magdi-car-fullscreen')");
 assert.equal(await car("return document.querySelector('#movie_player').getAttribute('data-mm-fullscreen')==='player'&&getComputedStyle(document.querySelector('#fixture-play')).visibility==='visible'"),true);
 await phone("Harness.command('fullscreen',0)");await until(car,"!document.getElementById('magdi-car-fullscreen')");pass('Desktop-style inner player retains its own controls without enlarging unrelated containers');
 await car("originalVideo.parentNode.removeAttribute('id');originalVideo.parentNode.className=''");
 await phone("Harness.command('fullscreen',0)");await until(car,"!!document.getElementById('magdi-car-fullscreen')");
 assert.equal(await car("return originalVideo.controls&&originalVideo.getAttribute('data-mm-fullscreen')==='video'"),true);
 await phone("Harness.command('fullscreen',0)");await until(car,"!document.getElementById('magdi-car-fullscreen')");assert.equal(await car('return originalVideo.controls'),false);pass('Unknown player layout uses reversible browser video controls instead of a control-free video');
 await phone("Harness.command('fullscreen',0)");await until(car,"!!document.getElementById('magdi-car-fullscreen')");await phone('Harness.toggleCar()');await until(phone,'!JSON.parse(Harness.state()).connected');await phone('Harness.toggleCar()');await sleep(600);
 assert.equal(await car("return !document.querySelector('video')&&!document.getElementById('magdi-car-fullscreen')"),true);pass('Disconnect removes the fullscreen player and reconnect does not autoplay');
 console.log(JSON.stringify({passed:checks.length,checks},null,2));
}finally{for(const ws of sockets)ws.close();}
