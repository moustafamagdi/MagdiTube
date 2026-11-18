// Real Android WebViews; launch debug ControllerHarnessActivity and forward CDP to 9226.
import assert from 'node:assert/strict';
const pages=await(await fetch('http://127.0.0.1:9226/json/list')).json();
const sockets=[],checks=[];
async function client(title){const page=pages.find(p=>p.title===title);assert(page,`Missing ${title}`);const ws=new WebSocket(page.webSocketDebuggerUrl);sockets.push(ws);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});let serial=0;const pending=new Map();ws.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(m.error):p.resolve(m.result);}};return async expression=>{const id=++serial;const result=await new Promise((resolve,reject)=>{pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method:'Runtime.evaluate',params:{expression:`(async()=>{${expression}})()`,awaitPromise:true,returnByValue:true,userGesture:true}}));});if(result.exceptionDetails)throw Error(result.exceptionDetails.exception?.description||result.exceptionDetails.text);return result.result.value;};}
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function until(run,expression){for(let i=0;i<100;i++){try{if(await run('return '+expression))return;}catch{}await sleep(150);}throw Error('Timeout: '+expression);}
function pass(name){checks.push(name);console.log('PASS '+name);}
try{
 const phone=await client('Phone Controller'),car=await client('Car Fixture'),probe=await client('Native CSP Probe');
 await until(phone,'JSON.parse(Harness.state()).connected');assert.equal(await phone('return JSON.parse(Harness.controls()).Stop'),false);assert.equal(await phone("return Harness.command('stop',0)"),'false');assert.equal(await car("return location.pathname==='/'"),true);pass('Idle Stop is disabled and rejected without blanking car Home');

 await until(probe,"violations.filter(v=>v.directive==='media-src').length>=3");
 assert.equal(await probe("return HTMLMediaElement.prototype.play.toString().includes('[native code]')&&[...document.querySelectorAll('video')].every(v=>v.paused&&v.getVideoPlaybackQuality().totalVideoFrames===0)"),true);pass('Android response CSP blocks network, data and blob video without any JS media overrides');
 await until(phone,'JSON.parse(Harness.state()).connected&&window.__magdiPhoneEpoch>=0');
 assert.equal(await phone('return Harness.muted()'),'true');pass('Android renderer mute is enforced on the phone');
 assert.equal(await phone('return Harness.cookies()'),'true');pass('Native cookie transfer preserves HttpOnly/Secure and propagates deletion');
 await phone("document.querySelectorAll('a')[2].click()");await until(phone,'JSON.parse(Harness.controls()).Stop');await phone("Harness.command('stop',0)");await sleep(2400);assert.equal(await car("return location.pathname==='/'&&!document.querySelector('video')"),true);assert.equal(await phone('return JSON.parse(Harness.controls()).Stop'),false);pass('Stop during loading returns car Home and cancels late start');
 await phone("document.querySelector('a').click()");
 await until(car,"document.querySelector('video')?.currentTime>0.5&&!document.querySelector('video').paused");
 await until(phone,"JSON.parse(Harness.state()).playback.title==='Fixture AAAAAAAAAAA'");
 assert.equal(await car("return location.hash"),'');
 await until(phone,"JSON.parse(Harness.controls()).Pause&&!JSON.parse(Harness.controls()).Play&&JSON.parse(Harness.controls())['Car fullscreen']");
 pass('Fragment-free navigation acknowledges the selected player and enables actual native phone controls');
 assert.equal(await car("return document.querySelector('video').getVideoPlaybackQuality().totalVideoFrames>0"),true);pass('Phone selection decodes video in the car and reports actual title/progress');
 assert.equal(await phone("return !document.querySelector('video')&&location.pathname==='/'"),true);pass('Phone remains on browsing page without local playback');
 await phone("Harness.command('pause',0)");await until(car,"document.querySelector('video').paused");await until(phone,"JSON.parse(Harness.state()).phase==='paused'&&JSON.parse(Harness.controls()).Play&&!JSON.parse(Harness.controls()).Pause");
 await phone("Harness.command('seek',10)");await until(car,"document.querySelector('video').currentTime>=9.5");
 await phone("Harness.command('play',0)");await until(car,"!document.querySelector('video').paused");await until(phone,"JSON.parse(Harness.controls()).Pause&&!JSON.parse(Harness.controls()).Play");pass('Pause, seek and play operate the car; Play/Pause buttons dim according to observed state');
 assert.equal(await phone("return Harness.command('next',0)"),'false');assert.equal(await phone("return Harness.command('previous',0)"),'false');pass('Unsupported next/previous are refused');
 await car("const p=document.createElement('div');p.id='movie_player';p.getPlayerResponse=()=>ytInitialPlayerResponse;document.body.append(p);for(const [cls,id] of [['ytp-next-button','BBBBBBBBBBB'],['ytp-prev-button','AAAAAAAAAAA']]){const b=document.createElement('button');b.className=cls;p.append(b);b.onclick=()=>{history.pushState({},'', '/watch?v='+id);ytInitialPlayerResponse.videoDetails={videoId:id,title:'Fixture '+id,author:'Test'};document.querySelector('video').currentTime=0;document.querySelector('video').play();};}");
 await until(phone,'JSON.parse(Harness.state()).playback.next');await phone("Harness.command('next',0)");await until(phone,"JSON.parse(Harness.state()).playback.title==='Fixture BBBBBBBBBBB'");await phone("Harness.command('previous',0)");await until(phone,"JSON.parse(Harness.state()).playback.title==='Fixture AAAAAAAAAAA'");pass('Supported next/previous use the actual player controls and update the phone');
 await phone("Harness.command('fullscreen',0)");await until(car,"!!document.getElementById('magdi-car-fullscreen')");await phone("Harness.command('fullscreen',0)");await until(car,"!document.getElementById('magdi-car-fullscreen')");pass('Remote car fullscreen toggles');
 await car("await document.querySelector('video').requestFullscreen()");
 await until(car,"!!document.fullscreenElement");
 const fullTime=await phone("return JSON.parse(Harness.state()).playback.time");
 await until(phone,`JSON.parse(Harness.state()).playback.time>${fullTime}+2&&JSON.parse(Harness.controls()).Pause`);
 await phone("Harness.command('pause',0)");await until(car,"document.querySelector('video').paused");await until(phone,"JSON.parse(Harness.state()).phase==='paused'&&JSON.parse(Harness.controls()).Play&&!JSON.parse(Harness.controls()).Pause");
 await phone("Harness.command('play',0)");await until(car,"!document.querySelector('video').paused");
 await car("await document.exitFullscreen()");pass('Native WebView video fullscreen preserves phone status, progress and play/pause controls');
 // Synthetic event/fault injection into the actual car WebView; assertions read native buttons.
 await car("document.querySelector('video').dispatchEvent(new Event('waiting'))");
 await until(phone,"JSON.parse(Harness.state()).phase==='buffering'&&JSON.parse(Harness.controls()).Pause&&!JSON.parse(Harness.controls()).Play");
 await car("document.querySelector('video').dispatchEvent(new Event('playing'))");await until(phone,"JSON.parse(Harness.state()).phase==='playing'");pass('Synthetic buffering is distinct from pause in native controls');
 await car("Object.defineProperty(document.querySelector('video'),'error',{configurable:true,value:{code:3}})");
 await until(phone,"JSON.parse(Harness.state()).phase==='error'&&!JSON.parse(Harness.controls()).Pause&&!JSON.parse(Harness.controls()).Play");
 await car("delete document.querySelector('video').error");await until(phone,"JSON.parse(Harness.state()).phase==='playing'");pass('Injected media error disables native transport and valid observations recover');
 await car("const v=document.querySelector('video');Object.defineProperty(v,'ended',{configurable:true,value:true});Object.defineProperty(v,'paused',{configurable:true,value:true})");
 await until(phone,"JSON.parse(Harness.state()).phase==='ended'&&JSON.parse(Harness.controls()).Play&&!JSON.parse(Harness.controls()).Pause");
 await car("const v=document.querySelector('video');delete v.ended;delete v.paused");await until(phone,"JSON.parse(Harness.state()).phase==='playing'");pass('Injected ended state offers supported replay in native controls');
 await car("document.querySelector('video').pause()");await until(phone,"JSON.parse(Harness.state()).phase==='paused'&&JSON.parse(Harness.controls()).Play");
 await car("window.savedSample=window.__magdiSample;window.__magdiSample=(...args)=>{const s=JSON.parse(savedSample(...args));s.controller.token='obsolete';return JSON.stringify(s)}");
 await until(phone,"!JSON.parse(Harness.state()).fresh&&!JSON.parse(Harness.controls()).Pause&&!JSON.parse(Harness.controls()).Play");
 await car("window.__magdiSample=window.savedSample");await until(phone,"JSON.parse(Harness.state()).fresh&&JSON.parse(Harness.controls()).Play");pass('Repeated rejected reports expire native controls; a valid current report restores them');
 await phone("Harness.command('stop',0)");await until(car,"!document.querySelector('video')");pass('Stop unloads the car media document');
 await phone("document.querySelector('a').click();document.querySelectorAll('a')[1].click();document.querySelector('a').click()");await until(car,"location.search.includes('AAAAAAAAAAA')&&document.querySelector('video')?.currentTime>0.2");await sleep(700);assert.equal(await car("return location.search.includes('AAAAAAAAAAA')"),true);pass('Rapid A/B/A selections execute only the final request');
 await phone("document.querySelectorAll('a')[2].click()");await sleep(100);const loadingRequest=await phone("return JSON.parse(Harness.state()).request");await phone("for(let i=0;i<10;i++)document.querySelectorAll('a')[2].click()");await sleep(150);assert.equal(await phone("return JSON.parse(Harness.state()).request"),loadingRequest);await until(car,"location.search.includes('SSSSSSSSSSS')&&document.querySelector('video')?.currentTime>0.2");pass('Repeated taps on the loading video preserve its request and allow startup');
 await phone('Harness.toggleCar()');await until(phone,'!JSON.parse(Harness.state()).connected');await until(car,"!document.querySelector('video')");
 await phone("document.querySelector('a').click()");await phone('Harness.toggleCar()');await until(phone,'JSON.parse(Harness.state()).connected');await sleep(700);assert.equal(await car("return !document.querySelector('video')"),true);pass('Disconnect stops playback; rejected selections do not queue or autoplay on reconnect');
 await sleep(600);await phone("document.querySelectorAll('a')[2].click()");await sleep(350);await phone('Harness.toggleCar()');await phone('Harness.toggleCar()');await until(phone,'JSON.parse(Harness.state()).connected');await sleep(2200);assert.equal(await car("return !document.querySelector('video')"),true);pass('Disconnect during slow loading invalidates late navigation and responses');
 await phone("document.querySelector('a').click()");await until(car,"document.querySelector('video')?.currentTime>0.3");
 // Attempt real local media using a fresh iframe realm, bypassing the top-level JS play override.
 await phone("let f=document.createElement('iframe');f.srcdoc='<video controls src=\"https://m.youtube.com/fixture.mp4\"></video>';document.body.append(f)");await sleep(500);
 const blocked=await phone("const v=document.querySelector('iframe').contentDocument.querySelector('video');try{await v.play()}catch{}return {paused:v.paused,frames:v.getVideoPlaybackQuality().totalVideoFrames}");assert.equal(blocked.paused,true);assert.equal(blocked.frames,0);pass('Native phone CSP/request policy blocks a fresh-realm media playback attempt');
 await phone('Harness.closePhone()');const before=await car("return document.querySelector('video').currentTime");await sleep(1200);assert.equal(await car(`return !document.querySelector('video').paused&&document.querySelector('video').currentTime>${before}`),true);pass('Closing the phone controller leaves available car playback running');
 console.log(JSON.stringify({passed:checks.length,checks},null,2));
}finally{for(const ws of sockets)ws.close();}
