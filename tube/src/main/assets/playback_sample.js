(function(){
 if(window.__magdiSample)return;
 const tracked=new WeakMap();
 function counters(v){
  if(!v)return null;
  let c=tracked.get(v);if(c)return c;
  c={waiting:0,stalled:0,loads:0};tracked.set(v,c);
  v.addEventListener('waiting',()=>c.waiting++);
  v.addEventListener('stalled',()=>c.stalled++);
  v.addEventListener('loadstart',()=>{c.waiting=0;c.stalled=0;c.loads++;});
  return c;
 }
 window.__magdiSample=function(detailed,artwork){
  const v=document.querySelector('video'),c=counters(v);
  const result={playing:!!v&&!v.paused&&!v.ended,time:v?Math.round(v.currentTime):0,ready:v?v.readyState:0,muted:v?v.muted:false,resumeWanted:window.__magdiPlayback?window.__magdiPlayback.state().wanted:false};
  if(artwork)result.artwork=window.__magdiArtwork?window.__magdiArtwork():null;
  if(detailed){
   let q=null;try{q=v&&typeof v.getVideoPlaybackQuality==='function'?v.getVideoPlaybackQuality():null;}catch(e){}
   result.performance={width:v?v.videoWidth:0,height:v?v.videoHeight:0,totalFrames:q?q.totalVideoFrames:null,droppedFrames:q?q.droppedVideoFrames:null,waiting:c?c.waiting:0,stalled:c?c.stalled:0,loads:c?c.loads:0};
  }
  return JSON.stringify(result);
 };
})();
