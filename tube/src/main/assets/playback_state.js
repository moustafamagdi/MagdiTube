(function(){
 if(window.__magdiPlayback)return;
 let covered=false,wanted=false,video=null,lastPointer=0,suppressed=false,interrupted=false,resuming=false,recentPause=0;
 function attach(){let v=document.querySelector('video');if(v===video)return;video=v;recentPause=0;wanted=!suppressed&&!!v&&!v.paused&&!v.ended;
  if(!v)return;
  v.addEventListener('play',()=>{if(suppressed){wanted=false;v.pause();}else if(video===v)wanted=true;});
  v.addEventListener('ended',()=>{if(video===v){wanted=false;recentPause=0;}});
  v.addEventListener('pause',()=>{if(video!==v)return;const manual=Date.now()-lastPointer<800;if(manual){wanted=false;recentPause=0;}else if(!interrupted&&!covered&&!document.hidden){recentPause=wanted?Date.now():0;wanted=false;}});
 }
 document.addEventListener('pointerdown',()=>{lastPointer=Date.now();},true);
 document.addEventListener('keydown',()=>{lastPointer=Date.now();},true);
 document.addEventListener('play',e=>{if(suppressed&&e.target&&typeof e.target.pause==='function'){wanted=false;e.target.pause();}},true);
 window.addEventListener('blur',()=>{attach();covered=true;},true);
 document.addEventListener('visibilitychange',()=>{if(document.hidden){attach();covered=true;}},true);
 new MutationObserver(()=>{if(!video||video.isConnected===false)attach();}).observe(document.documentElement,{childList:true,subtree:true});attach();
 window.__magdiPlayback={
  cover:()=>{attach();covered=true;return wanted;},
  stop:()=>{wanted=false;covered=false;recentPause=0;},
  interrupt:value=>{attach();interrupted=!!value;if(interrupted){if(recentPause&&Date.now()-recentPause<1500&&!suppressed)wanted=true;covered=true;recentPause=0;}return wanted;},
  suppress:value=>{suppressed=!!value;if(suppressed){wanted=false;covered=false;recentPause=0;document.querySelectorAll('video,audio').forEach(v=>v.pause());}},
  resume:()=>{attach();const should=!suppressed&&!interrupted&&covered&&wanted;if(!should||!video||video.ended||resuming)return 'no-resume';if(!video.paused){covered=false;return 'already-playing';}const target=video;resuming=true;target.play().then(()=>{if(video===target)covered=false;}).catch(()=>{}).finally(()=>{resuming=false;});return 'resume-requested';},
  state:()=>{attach();return {wanted:wanted,covered:covered};}
 };
})();
