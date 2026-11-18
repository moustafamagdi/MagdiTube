(()=>{
 'use strict';
 // Defence in depth; the Android response policy and renderer mute remain authoritative.
 const stop=v=>{try{v.pause();v.removeAttribute('src');v.querySelectorAll('source').forEach(s=>s.remove());}catch(_){}};
 HTMLMediaElement.prototype.play=function(){stop(this);return Promise.reject(new DOMException('Use the car player','NotAllowedError'));};
 for(const name of ['AudioContext','webkitAudioContext','MediaSource','ManagedMediaSource'])try{Object.defineProperty(window,name,{value:undefined,writable:false,configurable:false});}catch(_){}
 const protect=()=>{
  if(document.head&&!document.getElementById('magdi-media-policy')){const m=document.createElement('meta');m.id='magdi-media-policy';m.httpEquiv='Content-Security-Policy';m.content="media-src 'none'; object-src 'none'; worker-src 'none'";document.head.prepend(m);}
  document.querySelectorAll('video,audio').forEach(stop);
 };
 new MutationObserver(protect).observe(document,{childList:true,subtree:true});
 document.addEventListener('play',e=>stop(e.target),true);
 const select=url=>{try{const u=new URL(url,location.href);if(!['youtube.com','www.youtube.com','m.youtube.com','youtu.be'].includes(u.hostname))return false;return u.pathname==='/watch'&&u.searchParams.has('v')||/^\/(shorts|live)\/[\w-]{11}/.test(u.pathname)||u.hostname==='youtu.be';}catch(_){return false;}};
 window.__magdiPhoneEpoch=-1;let sequence=0;
 const send=url=>{if(!window.MagdiController)return;MagdiController.postMessage(JSON.stringify({url,epoch:window.__magdiPhoneEpoch,sequence:++sequence}));};
 document.addEventListener('click',e=>{const a=e.target.closest?.('a[href]');if(a&&select(a.href)){e.preventDefault();e.stopImmediatePropagation();send(a.href);}},true);
 // Some mobile YouTube cards use SPA navigation instead of an anchor click.
 for(const method of ['pushState','replaceState']){const original=history[method];history[method]=function(state,title,url){if(url&&select(url)){send(new URL(url,location.href).href);return;}return original.apply(this,arguments);};}
 protect();
})();
