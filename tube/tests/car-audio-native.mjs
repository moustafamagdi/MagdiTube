// Debug ControllerHarnessActivity; LIVE_YOUTUBE=1 requires --ez liveCar true.
// Default is synthetic YouTube UI state around a real Chromium video decoder.
import assert from 'node:assert/strict';
const live=process.env.LIVE_YOUTUBE==='1',sockets=[];
const wait=ms=>new Promise(r=>setTimeout(r,ms));
async function client(phone){
 let p;for(let n=0;n<50&&!p;n++){const pages=await(await fetch('http://127.0.0.1:9226/json/list')).json();
  p=pages.find(p=>phone?p.title==='Phone Controller':live?p.type==='page'&&p.url.startsWith('https://m.youtube.com/')&&p.title!=='Phone Controller'&&p.title!=='':p.title==='Car Fixture');if(!p)await wait(200);
 }assert(p,'Missing harness page');
 const ws=new WebSocket(p.webSocketDebuggerUrl);sockets.push(ws);await new Promise((r,j)=>{ws.onopen=r;ws.onerror=j;});let id=0;const pending=new Map();
 ws.onmessage=e=>{const m=JSON.parse(e.data),p=pending.get(m.id);if(p){pending.delete(m.id);clearTimeout(p.timer);m.error?p.reject(Error(JSON.stringify(m.error))):p.resolve(m.result);}};
 return async code=>{const key=++id;const r=await new Promise((resolve,reject)=>{const timer=setTimeout(()=>reject(Error('CDP timeout')),20000);pending.set(key,{resolve,reject,timer});ws.send(JSON.stringify({id:key,method:'Runtime.evaluate',params:{expression:`(async()=>{${code}})()`,returnByValue:true,awaitPromise:true}}));});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description);return r.result.value;};
}
async function until(fn,code){for(let i=0;i<150;i++){if(await fn('return '+code))return;await wait(200);}throw Error('Timeout: '+code);}
const quiet="!document.querySelector('video').muted&&document.querySelector('video').volume>0&&!document.getElementById('movie_player').isMuted()&&!document.querySelector('button.ytp-unmute')?.getClientRects().length";
try{
 const phone=await client(true),car=await client(false);
 await until(phone,'JSON.parse(Harness.state()).connected');
 await phone(`const a=document.querySelector('a');a.href='/watch?v=${live?'aqz-KE-bpKQ':'AAAAAAAAAAA'}';a.click();`);
 await until(car,"!!window.__magdiAudio&&document.querySelector('video')?.readyState>=3&&!document.querySelector('video').paused");
 await until(phone,'JSON.parse(Harness.controls()).Pause');
 if(!live)await car(`
  window.testVideo=document.querySelector('video');const p=testVideo.parentElement;p.id='movie_player';
  p.testMuted=true;const b=document.createElement('button');b.className='ytp-unmute';b.textContent='Unmute';p.appendChild(b);
  p.isMuted=()=>p.testMuted;p.getVolume=()=>testVideo.volume*100;p.setVolume=n=>testVideo.volume=n/100;
  p.unMute=()=>{p.testMuted=false;testVideo.muted=false;b.style.display='none';};
  b.onclick=()=>p.unMute();testVideo.muted=false;
 `);
 await until(car,quiet);console.log('PASS '+(live?'real YouTube':'synthetic UI')+': playing, HTML audio and player unmuted, prompt absent');
 await car("window.audioVideo=document.querySelector('video');window.audioSource=audioVideo.currentSrc;window.audioPlayer=document.getElementById('movie_player');audioPlayer.setVolume(35);");
 await wait(1700);assert.equal(await phone("return Harness.command('pause',0)"),'true');await until(car,'audioVideo.paused');await until(phone,'JSON.parse(Harness.controls()).Play');
 await car(live?'audioPlayer.mute();audioPlayer.setVolume(0);':"audioPlayer.testMuted=true;audioVideo.muted=true;audioVideo.volume=0;document.querySelector('button.ytp-unmute').style.display='block';");
 await wait(1700);assert(await car('return audioVideo.paused&&audioVideo.volume===0'),'No autoplay/repair during pause');
 assert.equal(await phone("return Harness.command('play',0)"),'true');await until(car,'!audioVideo.paused&&'+quiet);
 assert(await car('return audioVideo.volume>0&&audioVideo.volume<0.5'),'Restore positive volume, not maximum');
 console.log('PASS Pause/Play restores audio and player state without raising positive volume');
 await phone("Harness.command('fullscreen',0)");await until(car,'MMVideoFit.state().fullscreen');
 await car(live?'audioPlayer.mute();':"audioPlayer.testMuted=true;audioVideo.muted=true;document.querySelector('button.ytp-unmute').style.display='block';");
 await until(car,quiet);assert(await car('return audioVideo.currentSrc===audioSource&&!audioVideo.paused'));
 assert(await phone("return Harness.muted()==='true'&&!document.querySelector('video')&&!window.__magdiAudio"));
 console.log('PASS fullscreen keeps current stream audible; phone remains muted and has no audio helper/player');
 await phone("Harness.command('stop',0)");await until(phone,"!JSON.parse(Harness.controls()).Pause");
 console.log('PASS Stop cancels playback');
}finally{for(const ws of sockets)ws.close();}
