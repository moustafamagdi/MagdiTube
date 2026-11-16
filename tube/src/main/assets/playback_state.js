(function(){
 if(window.__magdiPlayback)return;
 let covered=false,wanted=false,video=null,lastPointer=0,suppressed=false;
 function attach(){let v=document.querySelector('video');if(v===video)return;video=v;wanted=!suppressed&&!!v&&!v.paused&&!v.ended;
  if(!v)return;
  v.addEventListener('play',()=>{if(suppressed){wanted=false;v.pause();}else if(video===v)wanted=true;});
  v.addEventListener('ended',()=>{if(video===v)wanted=false;});
  v.addEventListener('pause',()=>{if(video===v&&((!covered&&!document.hidden)||Date.now()-lastPointer<800))wanted=false;});
 }
 document.addEventListener('pointerdown',()=>{lastPointer=Date.now();},true);
 document.addEventListener('play',e=>{if(suppressed&&e.target&&typeof e.target.pause==='function'){wanted=false;e.target.pause();}},true);
 window.addEventListener('blur',()=>{attach();covered=true;},true);
 document.addEventListener('visibilitychange',()=>{if(document.hidden){attach();covered=true;}},true);
 new MutationObserver(()=>{if(!video||video.isConnected===false)attach();}).observe(document.documentElement,{childList:true,subtree:true});attach();
 window.__magdiPlayback={
  cover:()=>{attach();covered=true;return wanted;},
  stop:()=>{wanted=false;covered=false;},
  suppress:value=>{suppressed=!!value;if(suppressed){wanted=false;covered=false;document.querySelectorAll('video,audio').forEach(v=>v.pause());}},
  resume:()=>{attach();const should=!suppressed&&covered&&wanted;covered=false;if(!should||!video||video.ended||!video.paused)return 'no-resume';video.play().catch(()=>{});return 'resume-requested';},
  state:()=>{attach();return {wanted:wanted,covered:covered};}
 };
})();
