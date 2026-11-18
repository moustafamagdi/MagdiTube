(()=>{
 if(window.__magdiCar)return;
 let wanted=null,attempt=0;
 // Captured by Android at document start. YouTube can remove/change the URL fragment.
 const documentToken=typeof window.__magdiCarLease==='string'?window.__magdiCarLease:'';
 const token=()=>documentToken;
 const identity=()=>{try{return window.__magdiArtwork?.()||null;}catch(_){return null;}};
 const valid=()=>wanted&&token()===wanted.token&&new URL(location.href).searchParams.get('v')===wanted.id;
 window.__magdiCar={
  start(id,t){wanted={id,token:t};attempt=0;},
  cancel(){wanted=null;document.querySelectorAll('video,audio').forEach(v=>{v.pause();v.removeAttribute('src');v.load();});},
  command(action,value,t){
   if(t&&token()!==t)return false;
   const v=document.querySelector('video');if(!v)return false;
   if(action==='pause'){wanted=null;window.__magdiPlayback?.stop();v.pause();}
   else if(action==='play')v.play().catch(()=>{});
   else if(['seek','forward','rewind'].includes(action)){if(!v.seekable.length)return false;const next=action==='seek'?value:v.currentTime+(action==='forward'?10:-10);v.currentTime=Math.min(v.seekable.end(v.seekable.length-1),Math.max(v.seekable.start(0),next));}
   else if(action==='next'||action==='previous'){const art=identity();return !!art&&window.__magdiTransport?.skip(art.id,action==='next')==='requested';}
   return true;
  },
  state(){const v=document.querySelector('video'),art=identity();return {token:token(),id:art?.id||'',hasVideo:!!v,ready:v?v.readyState:0,playing:!!v&&!v.paused&&!v.ended,time:v?v.currentTime:0,duration:v&&Number.isFinite(v.duration)?v.duration:0,seekable:!!v&&v.seekable.length>0,title:art?.title||'',next:!!art?.next,previous:!!art?.previous};}
 };
 const timer=setInterval(()=>{
  if(!wanted)return;
  if(!valid()){wanted=null;return;}
  const v=document.querySelector('video'),art=identity();
  // Wait for the actual player to match, not just the address bar.
  if(v&&art?.id===wanted.id&&v.readyState>0){v.muted=false;v.play().catch(()=>{});wanted=null;}
  else if(++attempt>=120)wanted=null;
 },250);
 addEventListener('pagehide',()=>{clearInterval(timer);wanted=null;});
})();
