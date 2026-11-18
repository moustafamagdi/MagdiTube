(()=>{
 if(window.__magdiCar)return;
 let started=false,wanted=null,attempt=0,revoked=false,revision=0,tracked=null,waiting=false;
 // Immutable Android binding is installed before YouTube scripts, never taken from a URL.
 const documentToken=typeof window.__magdiCarLease==='string'?window.__magdiCarLease:'';
 const documentId=window.__magdiCarDocument||'';
 const identity=()=>{try{return window.__magdiArtwork?.()||null;}catch(_){return null;}};
 function video(){const v=document.querySelector('video');if(v&&v!==tracked){tracked=v;waiting=false;for(const name of ['waiting','stalled','playing','canplay','pause','ended','emptied'])v.addEventListener(name,()=>{if(tracked===v)waiting=name==='waiting'||name==='stalled';});}return v;}
 const matches=(t,d,id)=>!revoked&&t===documentToken&&d===documentId&&!!d&&id===(identity()?.id||'');
 window.__magdiCar={
  matches,
  guard(t,d,id,g){if(!matches(t,d,id)||g<revision)return false;revision=g;return true;},
  start(id,t){if(!revoked&&!started&&t===documentToken){started=true;wanted={id,token:t};attempt=0;}},
  revoke(){revoked=true;wanted=null;window.__magdiPlayback?.stop();},
  cancel(){this.revoke();document.querySelectorAll('video,audio').forEach(v=>{v.pause();v.removeAttribute('src');v.load();});},
  command(action,value,t,d,id,g){
   if(!this.guard(t,d,id,g))return false;
   const v=video();if(!v||v.error||!v.readyState)return false;
   if(action==='pause'){wanted=null;window.__magdiPlayback?.stop();v.pause();}
   else if(action==='play'){if(v.ended){if(!Number.isFinite(v.duration)||!v.seekable.length||v.seekable.start(0)>0)return false;v.currentTime=0;}v.play().catch(()=>{});}
   else if(['seek','forward','rewind'].includes(action)){if(!Number.isFinite(value)||!v.seekable.length)return false;const next=action==='seek'?value:v.currentTime+(action==='forward'?10:-10);v.currentTime=Math.min(v.seekable.end(v.seekable.length-1),Math.max(v.seekable.start(0),next));}
   else if(action==='next'||action==='previous')return window.__magdiTransport?.skip(id,action==='next')==='requested';
   else return false;
   return true;
  },
  state(){const v=video(),art=identity(),error=!!v?.error,ended=!!v?.ended,paused=!!v?.paused,ready=v?v.readyState:0;return {token:documentToken,document:documentId,id:art?.id||'',hasVideo:!!v,ready,playing:!!v&&!paused&&!ended&&!error,paused,ended,error,buffering:!!v&&!paused&&!ended&&!error&&(waiting||ready<3),replay:!!v&&Number.isFinite(v.duration)&&v.duration>0&&v.seekable.length>0&&v.seekable.start(0)===0,time:v?v.currentTime:0,duration:v&&Number.isFinite(v.duration)?v.duration:0,seekable:!!v&&!error&&ready>0&&v.seekable.length>0,title:art?.title||'',next:!!art?.next,previous:!!art?.previous};}
 };
 const timer=setInterval(()=>{
  if(!wanted||revoked)return;
  if(documentToken!==wanted.token||new URL(location.href).searchParams.get('v')!==wanted.id){wanted=null;return;}
  const v=video(),art=identity();
  if(v&&art?.id===wanted.id&&v.readyState>0){v.muted=false;v.play().catch(()=>{});wanted=null;}
  else if(++attempt>=120)wanted=null;
 },250);
 addEventListener('pagehide',()=>{clearInterval(timer);window.__magdiCar.revoke();});
})();
