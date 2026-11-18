package com.magdi.tube;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.json.JSONObject;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class CarControllerTest {
 static class Player implements CarController.Player {
  boolean car=true;int cancelled,selected,commands;long e,r;String id,action;
  public boolean isCarView(){return car;}
  public void cancelController(){cancelled++;}
  public void selectController(VideoSelection v,long epoch,long request){selected++;id=v.id;e=epoch;r=request;}
  public void commandController(String a,double value,long epoch,long request){commands++;action=a;}
 }
 private Player player;
 private final String a="https://m.youtube.com/watch?v=AAAAAAAAAAA",b="https://m.youtube.com/watch?v=BBBBBBBBBBB";
 @Before public void setup(){player=new Player();CarController.attach(player);}
 @After public void cleanup(){CarController.detach(player);}
 private JSONObject playing(long e,long r)throws Exception{return new JSONObject().put("token",e+"_"+r).put("document","fixture-document").put("paused",false).put("hasVideo",true).put("ready",4).put("playing",true).put("title","Fixture").put("id","AAAAAAAAAAA");}
 @Test public void unavailableAndUnreadyRejectWithoutQueue(){assertFalse(CarController.select(a,CarController.epoch()));CarController.ready(player);assertEquals(0,player.selected);CarController.detach(player);assertFalse(CarController.select(a,CarController.epoch()));CarController.attach(player);CarController.ready(player);assertEquals(0,player.selected);assertEquals("idle",CarController.state().optString("phase"));}
 @Test public void phoneCannotBecomePlayer(){CarController.detach(player);Player phone=new Player();phone.car=false;CarController.attach(phone);CarController.ready(phone);assertFalse(CarController.available());}
 @Test public void latestSelectionAndABARejectStaleReports()throws Exception{CarController.ready(player);long e=CarController.epoch();assertTrue(CarController.select(a,e));long old=player.r;CarController.select(b,e);CarController.select(a,e);assertFalse(CarController.accepts(player,e,old));CarController.report(player,e,player.r,playing(e,old));assertEquals("sending",CarController.state().optString("phase"));CarController.report(player,e,player.r,playing(e,player.r));assertEquals("playing",CarController.state().optString("phase"));}
 @Test public void disconnectDuringLoadInvalidatesCommandsAndCallbacks()throws Exception{CarController.ready(player);long e=CarController.epoch();CarController.select(a,e);long r=player.r;CarController.detach(player);assertEquals(1,player.cancelled);CarController.attach(player);CarController.ready(player);assertFalse(CarController.command("play",0,e,CarController.request(),CarController.owner()));assertFalse(CarController.accepts(player,e,r));CarController.report(player,e,r,playing(e,r));assertEquals("idle",CarController.state().optString("phase"));assertFalse(CarController.state().optBoolean("retry"));}
 @Test public void stopCancelsSelectionAndRetryRequiresExplicitSelection(){CarController.ready(player);long e=CarController.epoch();CarController.select(a,e);long r=player.r;assertTrue(CarController.command("stop",0,e,CarController.request(),CarController.owner()));assertFalse(CarController.accepts(player,e,r));assertFalse(CarController.command("retry",0,e,CarController.request(),CarController.owner()));assertEquals(1,player.cancelled);}
 @Test public void initialAcknowledgementRequiresSelectedPlayerButAllowsLaterNext()throws Exception{
  CarController.ready(player);long e=CarController.epoch();CarController.select(a,e);long r=player.r;
  assertEquals("current-loading",CarController.report(player,e,r,playing(e,r).put("id","BBBBBBBBBBB")));
  assertFalse(CarController.command("pause",0,e,CarController.request(),CarController.owner()));
  assertEquals("accepted",CarController.report(player,e,r,playing(e,r)));
  assertTrue(CarController.command("pause",0,e,CarController.request(),CarController.owner()));
  assertEquals("accepted",CarController.report(player,e,r,playing(e,r).put("id","BBBBBBBBBBB")));
  assertEquals("BBBBBBBBBBB",CarController.state().getJSONObject("playback").getString("id"));
 }
 @Test public void stopRejectsPreviousDocumentEvenWhenSampleUsesCurrentGeneration()throws Exception{
  CarController.ready(player);long e=CarController.epoch();CarController.select(a,e);long r=player.r;
  CarController.report(player,e,r,playing(e,r));CarController.command("stop",0,e,CarController.request(),CarController.owner());
  assertEquals("document-mismatch",CarController.report(player,e,CarController.request(),playing(e,r)));
  assertEquals("idle",CarController.state().optString("phase"));assertFalse(CarController.command("play",0,e,CarController.request(),CarController.owner()));
 }
 @Test public void commandsRequireObservedVideoAndSupportedTransport()throws Exception{CarController.ready(player);long e=CarController.epoch();assertFalse(CarController.command("play",0,e,CarController.request(),CarController.owner()));CarController.report(player,e,CarController.request(),playing(e,CarController.request()).put("token",""));assertTrue(CarController.command("pause",0,e,CarController.request(),CarController.owner()));assertFalse(CarController.command("next",0,e,CarController.request(),CarController.owner()));assertFalse(CarController.command("lay",0,e,CarController.request(),CarController.owner()));assertFalse(CarController.command("seek",Double.NaN,e,CarController.request(),CarController.owner()));assertEquals(1,player.commands);}
 @Test public void videoUrlsAreStrictAndDoNotForwardSecrets(){assertNull(VideoSelection.parse("https://m.youtube.com.evil/watch?v=AAAAAAAAAAA"));assertNull(VideoSelection.parse("https://user:secret@m.youtube.com/watch?v=AAAAAAAAAAA"));assertNull(VideoSelection.parse("javascript:alert(1)"));assertNull(VideoSelection.parse("https://m.youtube.com/watch?v=x"));assertEquals(a,VideoSelection.parse(a+"&token=secret#anything").url);assertEquals("AAAAAAAAAAA",VideoSelection.parse("https://youtu.be/AAAAAAAAAAA").id);}
 @Test public void nativePhonePolicyRejectsMediaAndCspForbidsBlobData(){assertTrue(PhoneMediaPolicy.blocked(android.net.Uri.parse("https://r1.googlevideo.com/videoplayback?a=1")));assertTrue(PhoneMediaPolicy.blocked(android.net.Uri.parse("https://m.youtube.com/youtubei/v1/player")));assertFalse(PhoneMediaPolicy.blocked(android.net.Uri.parse("https://m.youtube.com/results?search_query=test")));assertTrue(PhoneMediaPolicy.html("<video autoplay></video>").getResponseHeaders().get("Content-Security-Policy").contains("media-src 'none'"));}
}
