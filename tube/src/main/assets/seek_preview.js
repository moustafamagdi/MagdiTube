(()=>{
 if(window.__mmSeekPreview)return;window.__mmSeekPreview=true;
 const selector='yt-player-storyboard .ytPlayerStoryboardStoryboardImageWrapper';
 const css=document.createElement('style');css.textContent=selector+'{pointer-events:auto!important;cursor:pointer}';document.head.append(css);
 let gesture=null,lastCommit=0;
 const target=e=>e.target?.closest?.(selector);
 const stamp=el=>el.closest('yt-player-storyboard')?.querySelector('.ytPlayerStoryboardTimestamp')?.textContent?.trim()||'';
 function seconds(text){const latin=text.replace(/[٠-٩]/g,c=>String(c.charCodeAt(0)-1632)).replace(/[۰-۹]/g,c=>String(c.charCodeAt(0)-1776)).replace(/[\u200e\u200f\u061c]/g,'');if(!/^\d{1,3}:\d{2}(?::\d{2})?$/.test(latin))return NaN;const parts=latin.split(':').map(Number);if(parts.slice(1).some(n=>n>=60))return NaN;return parts.reduce((n,p)=>n*60+p,0);}
 function seek(el){const v=document.querySelector('video'),time=seconds(stamp(el));if(!v||!window.__magdiCar?.audioAllowed(v)||!Number.isFinite(v.duration)||!Number.isFinite(time)||time<0||time>=v.duration)return false;const root=['#player-container-id','.html5-video-player','#movie_player','#player'].map(s=>v.closest(s)).find(r=>r&&r.querySelectorAll('video').length===1);if(!root?.contains(el))return false;let available=false;for(let i=0;i<v.seekable.length;i++)if(time>=v.seekable.start(i)&&time<=v.seekable.end(i))available=true;if(!available)return false;v.currentTime=time;v.play().catch(()=>{});return true;}
 document.addEventListener('pointerdown',e=>{const el=target(e);gesture=el?{el,x:e.clientX,y:e.clientY,moved:false}:null;if(el){e.preventDefault();e.stopImmediatePropagation();}},true);
 document.addEventListener('pointermove',e=>{if(gesture&&Math.hypot(e.clientX-gesture.x,e.clientY-gesture.y)>12)gesture.moved=true;},true);
 document.addEventListener('pointerup',e=>{if(gesture&&!gesture.moved&&target(e)===gesture.el&&seek(gesture.el)){lastCommit=Date.now();e.preventDefault();e.stopImmediatePropagation();}if(!gesture?.moved)gesture=null;},true);
 document.addEventListener('pointercancel',()=>{if(gesture)gesture.moved=true;},true);
 document.addEventListener('click',e=>{const el=target(e);if(el&&Date.now()-lastCommit<400){e.preventDefault();e.stopImmediatePropagation();return;}if(!el||e.button!==0||gesture&&(gesture.el!==el||gesture.moved))return;if(seek(el)){e.preventDefault();e.stopImmediatePropagation();}gesture=null;},true);
 document.addEventListener('keydown',e=>{const el=target(e);if(el&&(e.key==='Enter'||e.key===' ')&&seek(el)){e.preventDefault();e.stopImmediatePropagation();}},true);
 function accessible(){for(const el of document.querySelectorAll(selector)){el.setAttribute('role','button');el.tabIndex=0;el.setAttribute('aria-label',(/^(ar|fa)/.test(document.documentElement.lang)?'تشغيل من ':'Play from ')+stamp(el));el.closest('.ytPlayerStoryboardHost')?.removeAttribute('aria-hidden');}}
 new MutationObserver(accessible).observe(document.body,{childList:true,subtree:true,characterData:true});accessible();
})();
