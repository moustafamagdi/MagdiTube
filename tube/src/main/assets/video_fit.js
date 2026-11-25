(()=>{
 if(window.MMVideoFit)return;
 let inline=false,fill=false,previous=null,previousRoot=null,saved=new Map(),path=[],observer=null;
 const video=()=>document.querySelector('video');
 const fullscreen=v=>!!v&&(inline||!!document.fullscreenElement&&(document.fullscreenElement===v||document.fullscreenElement.contains(v)));
 function restore(){
  observer?.disconnect();observer=null;
  for(const [element,style] of saved){if(style===null)element.removeAttribute('style');else element.setAttribute('style',style);}
  saved=new Map();path=[];previous=null;previousRoot=null;
 }
 function patch(element,properties){
  // Save the complete declaration so shorthand overrides also restore original longhands.
  if(!saved.has(element))saved.set(element,element.getAttribute('style'));
  for(const [name,value] of Object.entries(properties))if(element.style.getPropertyValue(name)!==value||element.style.getPropertyPriority(name)!=='important')element.style.setProperty(name,value,'important');
 }
 function apply(){
  const v=video(),root=document.fullscreenElement;
  const active=!!v&&fullscreen(v)&&fill;
  const next=[];
  // Native YouTube fullscreen may promote a container, leaving fitted pixel dimensions
  // on the media wrappers. Stretch only that video's ancestor path; never scale controls.
  if(active&&!inline&&root&&root!==v)for(let el=v.parentElement;el&&el!==root;el=el.parentElement)next.push(el);
  if(previous&&(!active||previous!==v||previousRoot!==root||path.length!==next.length||path.some((el,i)=>el!==next[i])))restore();
  if(!active)return;
  previous=v;previousRoot=root;path=next;
  patch(v,{'object-fit':'cover','object-position':'50% 50%'});
  if(!inline&&root&&root!==v){
   const box={inset:'0px',width:'100%',height:'100%','min-width':'0px','min-height':'0px','max-width':'none','max-height':'none',margin:'0px',padding:'0px',border:'0px','box-sizing':'border-box','aspect-ratio':'auto',transform:'none',translate:'none',rotate:'none',scale:'none'};
   patch(root,{...box,position:'fixed',width:'100vw',height:'100vh',overflow:'visible'});
   for(const el of path)patch(el,{...box,display:'block',position:'absolute',filter:'none','backdrop-filter':'none',perspective:'none',contain:'none','content-visibility':'visible',clip:'auto','clip-path':'none',mask:'none',overflow:'visible'});
   patch(v,{...box,position:'absolute','z-index':'0'});
  }
  if(!observer){
   observer=new MutationObserver(apply);
   for(const el of saved.keys())observer.observe(el,{attributes:true,attributeFilter:['style']});
  }
 }
 window.MMVideoFit={
  inline(value){inline=!!value;apply();},
  set(value){if(typeof value!=='boolean'||!fullscreen(video())||!video()?.readyState)return false;fill=value;apply();window.dispatchEvent(new Event('mmvideofit'));return true;},
  state(){apply();return {fullscreen:fullscreen(video()),fill:fill&&fullscreen(video())};},
  reset(){inline=false;fill=false;restore();}
 };
 // Mobile YouTube portals can be inserted outside the native fullscreen subtree.
 // Move only popup containers (never the player/video), and restore their exact location on exit.
 const portals='ytm-menu-popup-renderer,ytm-bottom-sheet-renderer,ytm-player-settings,ytm-popup-container';
 const moved=new Map();let menuObserver=null,menuRoot=null;
 function restoreMenus(){menuObserver?.disconnect();menuObserver=null;for(const [el,place] of moved){if(place.parent.isConnected)place.parent.insertBefore(el,place.next?.parentNode===place.parent?place.next:null);}moved.clear();menuRoot=null;}
 function syncMenus(){const root=document.fullscreenElement;if(root!==menuRoot)restoreMenus();if(!root||root.tagName==='VIDEO')return;menuRoot=root;for(const el of document.querySelectorAll(portals)){if(root.contains(el)||el.contains(root)||!el.isConnected)continue;moved.set(el,{parent:el.parentNode,next:el.nextSibling});root.appendChild(el);}if(!menuObserver){menuObserver=new MutationObserver(syncMenus);menuObserver.observe(document.body,{childList:true,subtree:true});}}
 document.addEventListener('fullscreenchange',()=>{apply();syncMenus();window.dispatchEvent(new Event('mmvideofit'));});
 window.addEventListener('pagehide',restoreMenus);
 window.addEventListener('resize',apply);
 window.addEventListener('pagehide',()=>window.MMVideoFit.reset());
})();
