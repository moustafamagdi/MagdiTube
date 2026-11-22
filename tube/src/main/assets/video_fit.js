(()=>{
 if(window.MMVideoFit)return;
 let inline=false,fill=false,previous=null,saved=[];
 const video=()=>document.querySelector('video');
 const fullscreen=v=>!!v&&(inline||!!document.fullscreenElement&&(document.fullscreenElement===v||document.fullscreenElement.contains(v)));
 function restore(){if(previous)for(const [name,value,priority] of saved){if(value)previous.style.setProperty(name,value,priority);else previous.style.removeProperty(name);}previous=null;saved=[];}
 function apply(){
  const v=video();if(previous&&(!fullscreen(v)||previous!==v||!fill))restore();
  if(v&&fullscreen(v)&&fill){
   if(previous!==v){previous=v;saved=['object-fit','object-position'].map(name=>[name,v.style.getPropertyValue(name),v.style.getPropertyPriority(name)]);}
   v.style.setProperty('object-fit','cover','important');v.style.setProperty('object-position','50% 50%','important');
  }
 }
 window.MMVideoFit={
  inline(value){inline=!!value;apply();},
  set(value){if(typeof value!=='boolean'||!fullscreen(video())||!video()?.readyState)return false;fill=value;apply();window.dispatchEvent(new Event('mmvideofit'));return true;},
  state(){apply();return {fullscreen:fullscreen(video()),fill:fill&&fullscreen(video())};},
  reset(){inline=false;fill=false;restore();}
 };
 document.addEventListener('fullscreenchange',()=>{apply();window.dispatchEvent(new Event('mmvideofit'));});
 window.addEventListener('resize',apply);
 window.addEventListener('pagehide',()=>window.MMVideoFit.reset());
})();
