(()=>{
 if(window.__magdiCar)return;
 let started=false,wanted=null,attempt=0,revoked=false,revision=0,tracked=null,waiting=false;
 // Immutable Android binding is installed before YouTube scripts, never taken from a URL.
 const documentToken=typeof window.__magdiCarLease==='string'?window.__magdiCarLease:'';
 const documentId=window.__magdiCarDocument||'';
 const identity=()=>{try{return window.__magdiArtwork?.()||null;}catch(_){return null;}};
 function video(){const v=document.querySelector('video');if(v&&v!==tracked){tracked=v;waiting=false;for(const name of ['waiting','stalled','playing','canplay','pause','ended','emptied'])v.addEventListener(name,()=>{if(tracked===v)waiting=name==='waiting'||name==='stalled';});}return v;}
 const matches=(t,d,id)=>!revoked&&t===documentToken&&d===documentId&&!!d&&id===(identity()?.id||'');
 // Promote only the selected player (including its controls), never unrelated containers. Keep
 // the media element in place: reparenting it can detach Chromium's video surface.
 let fullscreenVideo=null,fullscreenStyle=null,fullscreenMarks=[],originalControls;
 function exitFullscreen(){
  window.MMVideoFit?.inline(false);
  if(!fullscreenStyle&&!fullscreenMarks.length)return true;
  fullscreenStyle?.remove();fullscreenStyle=null;
  if(fullscreenVideo&&originalControls!==undefined){if(originalControls===null)fullscreenVideo.removeAttribute('controls');else fullscreenVideo.setAttribute('controls',originalControls);}
  fullscreenVideo=null;originalControls=undefined;
  for(const [el,old] of fullscreenMarks){if(old===null)el.removeAttribute('data-mm-fullscreen');else el.setAttribute('data-mm-fullscreen',old);}
  fullscreenMarks=[];window.dispatchEvent(new Event('resize'));return true;
 }
 function playerRoot(v){
  // Mobile controls can be siblings of the inner html5-video-player. Only use
  // a known container that actually owns this single video, never a page-wide id lookup.
  for(const selector of ['#player-container-id','.html5-video-player','#movie_player','#player']){
   const root=v.closest(selector);if(root&&root.querySelectorAll('video').length===1)return root;
  }
  return v;
 }
 function enterFullscreen(){
  const v=video();if(!v||!v.isConnected||v.error||!v.readyState||document.fullscreenElement)return false;
  exitFullscreen();fullscreenVideo=v;const root=playerRoot(v);
  if(root===v){originalControls=v.getAttribute('controls');v.controls=true;}
  let inside=true;
  for(let el=v;el;el=el.parentElement){
   fullscreenMarks.push([el,el.getAttribute('data-mm-fullscreen')]);
   el.setAttribute('data-mm-fullscreen',el===v?'video':el===root?'player':inside?'media':'path');
   if(el===root)inside=false;
  }
  fullscreenStyle=document.createElement('style');fullscreenStyle.id='magdi-car-fullscreen';
  fullscreenStyle.textContent=`
   html,body{overflow:hidden!important;background:#000!important}
   body *:not([data-mm-fullscreen]):not([data-mm-fullscreen="player"] *){visibility:hidden!important}
   [data-mm-fullscreen]{visibility:visible!important;opacity:1!important;transform:none!important;translate:none!important;rotate:none!important;scale:none!important;filter:none!important;backdrop-filter:none!important;perspective:none!important;contain:none!important;content-visibility:visible!important;clip:auto!important;clip-path:none!important;mask:none!important;overflow:visible!important;isolation:auto!important;will-change:auto!important}
   [data-mm-fullscreen="path"]{display:block!important;position:static!important;z-index:auto!important;width:auto!important;height:auto!important;min-width:0!important;min-height:0!important;max-width:none!important;max-height:none!important}
   [data-mm-fullscreen="player"]{display:block!important;position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;min-width:0!important;min-height:0!important;max-width:none!important;max-height:none!important;margin:0!important;padding:0!important;border:0!important;box-sizing:border-box!important;z-index:1000!important;background:#000!important}
   [data-mm-fullscreen="media"]{display:block!important;position:absolute!important;inset:0!important;width:100%!important;height:100%!important;min-width:0!important;min-height:0!important;max-width:none!important;max-height:none!important;margin:0!important;padding:0!important;z-index:auto!important}
   [data-mm-fullscreen="video"]{display:block!important;position:${root===v?'fixed':'absolute'}!important;inset:0!important;width:100vw!important;height:100vh!important;min-width:0!important;min-height:0!important;max-width:none!important;max-height:none!important;margin:0!important;padding:0!important;border:0!important;box-sizing:border-box!important;object-fit:contain!important;z-index:${root===v?'2147483647':'auto'}!important;background:#000!important}
  `;
  fullscreenStyle.textContent+=`
   :is(ytm-menu-popup-renderer,ytm-bottom-sheet-renderer,ytm-player-settings,ytm-popup-container,tp-yt-paper-dialog), :is(ytm-menu-popup-renderer,ytm-bottom-sheet-renderer,ytm-player-settings,ytm-popup-container,tp-yt-paper-dialog) *{visibility:visible!important}
   :is(ytm-menu-popup-renderer,ytm-bottom-sheet-renderer,ytm-player-settings,ytm-popup-container,tp-yt-paper-dialog){z-index:2147483647!important}
   body *:has(:is(ytm-menu-popup-renderer,ytm-bottom-sheet-renderer,ytm-player-settings,ytm-popup-container,tp-yt-paper-dialog)):not([data-mm-fullscreen]){isolation:auto!important;transform:none!important;contain:none!important;overflow:visible!important;z-index:2147483647!important}
  `;
  document.head.appendChild(fullscreenStyle);window.dispatchEvent(new Event('resize'));
  const r=v.getBoundingClientRect();
  if(r.width<innerWidth-2||r.height<innerHeight-2||!root.contains(document.elementFromPoint(innerWidth/2,innerHeight/2))){exitFullscreen();return false;}
  window.MMVideoFit?.inline(true);return true;
 }
 window.__magdiCar={
  matches,
  audioAllowed:v=>!revoked&&v?.isConnected!==false&&document.querySelector('video')===v,
  fullscreen(enabled,t,d,id,g){if(!this.guard(t,d,id,g))return false;try{return enabled?enterFullscreen():exitFullscreen();}catch(_){exitFullscreen();return false;}},
  exitFullscreen,
  setFill(enabled,t,d,id,g){return this.guard(t,d,id,g)&&!!window.MMVideoFit?.set(enabled);},
  guard(t,d,id,g){if(!matches(t,d,id)||g<revision)return false;revision=g;return true;},
  start(id,t){if(!revoked&&!started&&t===documentToken){started=true;wanted={id,token:t};attempt=0;}},
  revoke(){exitFullscreen();revoked=true;wanted=null;window.__magdiAudio?.stop();window.__magdiPlayback?.stop();},
  cancel(){this.revoke();document.querySelectorAll('video,audio').forEach(v=>{v.pause();v.removeAttribute('src');v.load();});},
  command(action,value,t,d,id,g){
   if(!this.guard(t,d,id,g))return false;
   const v=video();if(!v||v.error||!v.readyState)return false;
   if(action==='pause'){wanted=null;window.__magdiPlayback?.stop();v.pause();}
   else if(action==='play'){if(v.ended){if(!Number.isFinite(v.duration)||!v.seekable.length||v.seekable.start(0)>0)return false;v.currentTime=0;}window.__magdiAudio?.prepare(v);v.play().catch(()=>{});}
   else if(['seek','forward','rewind'].includes(action)){if(!Number.isFinite(value)||!v.seekable.length)return false;const next=action==='seek'?value:v.currentTime+(action==='forward'?10:-10);v.currentTime=Math.min(v.seekable.end(v.seekable.length-1),Math.max(v.seekable.start(0),next));}
   else if(action==='next'||action==='previous')return window.__magdiTransport?.skip(id,action==='next')==='requested';
   else return false;
   return true;
  },
  state(){const v=video(),art=identity(),error=!!v?.error,ended=!!v?.ended,paused=!!v?.paused,ready=v?v.readyState:0;return {...(window.MMVideoFit?.state()||{fullscreen:false,fill:false}),token:documentToken,document:documentId,id:art?.id||'',hasVideo:!!v,ready,playing:!!v&&!paused&&!ended&&!error,paused,ended,error,buffering:!!v&&!paused&&!ended&&!error&&(waiting||ready<3),replay:!!v&&Number.isFinite(v.duration)&&v.duration>0&&v.seekable.length>0&&v.seekable.start(0)===0,time:v?v.currentTime:0,duration:v&&Number.isFinite(v.duration)?v.duration:0,seekable:!!v&&!error&&ready>0&&v.seekable.length>0,title:art?.title||'',next:!!art?.next,previous:!!art?.previous};}
 };
 const timer=setInterval(()=>{
  if(!wanted||revoked)return;
  if(documentToken!==wanted.token||new URL(location.href).searchParams.get('v')!==wanted.id){wanted=null;return;}
  const v=video(),art=identity();
  if(v&&art?.id===wanted.id&&v.readyState>0){window.__magdiAudio?.prepare(v);v.play().catch(()=>{});wanted=null;}
  else if(++attempt>=120)wanted=null;
 },250);
 addEventListener('pagehide',()=>{clearInterval(timer);window.__magdiCar.revoke();});
})();
