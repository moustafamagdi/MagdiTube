(()=>{
 'use strict';
 // Injected only in the car document. Never enable audio in the phone controller.
 if(window.__magdiAudio||!window.__magdiCar)return;
 const records=new WeakMap();let stopped=false,queued=false;
 const allowed=v=>!stopped&&v&&window.__magdiCar.audioAllowed(v)&&!v.ended&&!v.error;
 function record(v){let r=records.get(v);if(!r){r={attempts:0,last:-Infinity,volume:v.volume>0?v.volume:1,clicked:new WeakSet()};records.set(v,r);}return r;}
 function visible(b){if(!b||b.disabled||!b.getClientRects().length)return false;const s=getComputedStyle(b);return s.display!=='none'&&s.visibility!=='hidden'&&s.opacity!=='0';}
 function sync(v=document.querySelector('video'),preparing=false){
  if(!allowed(v)||(!preparing&&v.paused))return false;
  const r=record(v);if(v.volume>0)r.volume=v.volume;
  const root=v.closest('#movie_player')||v.closest('.html5-video-player');
  const player=root&&root.querySelectorAll('video').length===1?root:null;
  let apiMuted=false,apiSilent=false;
  try{apiMuted=player?.isMuted?.()===true;apiSilent=player?.getVolume?.()===0;}catch(_){}
  // Only YouTube's explicit unmute prompt; never a toggle, overlay play button or text match.
  const prompt=player?.querySelector('button.ytp-unmute,button.ytp-unmute-button');
  const needsPrompt=visible(prompt);
  const needsRepair=v.muted||v.volume===0||apiMuted||apiSilent||needsPrompt;
  if(!needsRepair){r.attempts=0;return true;}
  if(Date.now()-r.last<1000||r.attempts>=4)return false;
  r.last=Date.now();r.attempts++;
  // YouTube owns its muted state and Unmute UI. Updating HTMLMediaElement alone
  // can leave that state muted even while audio is already playing.
  try{if((apiMuted||v.muted||needsPrompt)&&typeof player?.unMute==='function')player.unMute();
   if(apiSilent&&typeof player?.setVolume==='function')player.setVolume(r.volume*100);
  }catch(_){}
  if(!allowed(v))return false;
  if(visible(prompt)&&!r.clicked.has(prompt)){r.clicked.add(prompt);prompt.click();}
  if(!allowed(v))return false;
  v.defaultMuted=false;if(v.muted)v.muted=false;if(v.volume===0)v.volume=r.volume;
  return !v.muted&&v.volume>0;
 }
 function schedule(){if(queued||stopped)return;queued=true;setTimeout(()=>{queued=false;sync();},0);}
 function event(e){const v=e.target;if(!allowed(v))return;
  if(e.type==='play'||e.type==='loadedmetadata')records.delete(v);
  schedule();
 }
 const events=['play','playing','loadedmetadata','volumechange'];for(const name of events)document.addEventListener(name,event,true);
 // Bounded repair attempts per playing item, including a late-mounted YouTube prompt.
 const timer=setInterval(()=>sync(),1500);
 window.__magdiAudio={prepare:v=>sync(v,true),sync,stop(){stopped=true;clearInterval(timer);for(const name of events)document.removeEventListener(name,event,true);}};
 addEventListener('pagehide',()=>window.__magdiAudio.stop(),{once:true});sync();
})();
