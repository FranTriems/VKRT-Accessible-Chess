#include <jni.h>
#include <algorithm>
#include <array>
#include <stdexcept>
#include <string>
#include "chess.hpp"

namespace {
std::string input(JNIEnv* env,jstring value,std::size_t limit) {
  if(!value||static_cast<std::size_t>(env->GetStringLength(value))>limit)throw std::invalid_argument("input");
  const char* raw=env->GetStringUTFChars(value,nullptr);
  if(!raw)throw std::runtime_error("JNI allocation");
  struct Release {
    JNIEnv* env; jstring value; const char* raw;
    ~Release(){env->ReleaseStringUTFChars(value,raw);}
  } release{env,value,raw};
  std::string text(raw);
  if(text.size()>limit)throw std::invalid_argument("input");
  for(unsigned char c:text)if(c>127||c==0)throw std::invalid_argument("ASCII input");
  return text;
}
jobjectArray output(JNIEnv* env,const std::array<std::string,9>& data) {
  jclass type=env->FindClass("java/lang/String");if(!type)return nullptr;
  jobjectArray out=env->NewObjectArray(9,type,nullptr);env->DeleteLocalRef(type);
  if(!out)return nullptr;
  for(jsize i=0;i<9;++i) {
    jstring value=env->NewStringUTF(data[static_cast<std::size_t>(i)].c_str());
    if(!value)return nullptr;
    env->SetObjectArrayElement(out,i,value);env->DeleteLocalRef(value);
    if(env->ExceptionCheck())return nullptr;
  }
  return out;
}
void exception(JNIEnv* env,const char* type,const char* message) noexcept {
  if(env->ExceptionCheck())return;
  jclass cls=env->FindClass(type);
  if(cls){env->ThrowNew(cls,message);env->DeleteLocalRef(cls);}
}
}
extern "C" JNIEXPORT jobjectArray JNICALL
Java_org_vkrt_accessiblechess_MainActivity_nativeRequest(JNIEnv* env,jclass,jstring state,jstring action,jstring argument) {
  try {
    auto saved=input(env,state,vkrt::chess::Session::max_save_bytes);
    auto op=input(env,action,16),arg=input(env,argument,32);
    auto session=saved.empty()?std::optional<vkrt::chess::Session>{vkrt::chess::Session(*vkrt::chess::Game::from_fen(vkrt::chess::Game::start_fen))}:vkrt::chess::Session::load(saved);
    if(!session)throw std::invalid_argument("Invalid saved game");
    std::string message;
    if(op=="play") {
      message=arg+" "+session->game().san(arg);
      if(!session->play(arg))throw std::invalid_argument("Illegal move, finished game or history limit");
    } else if(op=="undo") {
      if(!session->undo())throw std::invalid_argument("No move to undo");
    } else if(op=="claim") {
      if(!session->claim(arg))throw std::invalid_argument("Draw cannot be claimed");
    } else if(op=="propose"||op=="simple") {
      if(session->status()!="ONGOING"&&session->status()!="CHECK")throw std::invalid_argument("Finished game");
      if(op=="simple") {
        std::vector<std::string> moves;
        for(auto m:session->game().legal())moves.push_back(vkrt::chess::Game::uci(m));
        if(moves.empty())throw std::invalid_argument("No legal move");
        arg=*std::min_element(moves.begin(),moves.end());
      }
      auto san=session->game().san(arg);
      if(san.empty())throw std::invalid_argument("Illegal move");
      message=arg+" "+san;
    } else if(op!="snapshot")throw std::invalid_argument("Unknown command");
    const auto& game=session->game();
    std::string legal,board;auto status=session->status();
    if(status=="ONGOING"||status=="CHECK")for(auto move:game.legal()) {
      if(!legal.empty())legal+='\n';
      legal+=vkrt::chess::Game::uci(move);
    }
    for(int r=0;r<8;++r)for(int f=0;f<8;++f) {
      if(!board.empty())board+='\n';
      board+=static_cast<char>('a'+f);board+=static_cast<char>('8'-r);board+='|';board+=game.squares()[r*16+f];
    }
    return output(env,{session->save(),game.fen(),status,legal,board,session->history(),session->pgn(),message,session->can_claim((op=="propose"||op=="simple")?arg:"")?"1":"0"});
  } catch(const std::bad_alloc&) {exception(env,"java/lang/OutOfMemoryError","Native allocation failed");}
    catch(const std::exception&) {exception(env,"java/lang/IllegalArgumentException","Invalid game or action");}
    catch(...) {exception(env,"java/lang/IllegalStateException","Native operation failed");}
  return nullptr;
}
