#include "chess.hpp"
#include <algorithm>
#include <cctype>
#include <charconv>
#include <sstream>
#include <string>
#include <vector>

namespace vkrt::chess {
namespace {
constexpr bool on(int x) { return x >= 0 && x < 128 && !(x & 0x88); }
int color(char p) { return p == '.' ? 0 : (p >= 'A' && p <= 'Z' ? 1 : -1); }
char lower(char p) { return static_cast<char>(std::tolower(static_cast<unsigned char>(p))); }
std::string square(int i) { return {static_cast<char>('a' + (i & 7)),static_cast<char>('8' - (i >> 4))}; }
int parse_square(std::string_view s) { return s.size() != 2 || s[0]<'a'||s[0]>'h'||s[1]<'1'||s[1]>'8' ? -1 : (8-(s[1]-'0'))*16+(s[0]-'a'); }
constexpr int knight[] = {-33,-31,-18,-14,14,18,31,33};
constexpr int bishop[] = {-17,-15,15,17};
constexpr int rook[] = {-16,-1,1,16};
constexpr int king[] = {-17,-16,-15,-1,1,15,16,17};
}
std::optional<Game> Game::from_fen(std::string_view fen) {
  if(fen.empty()||fen.size()>256)return std::nullopt;
  Game s; s.b_.fill('.');
  std::istringstream in{std::string(fen)};
  std::string pos,turn,cast,ep,half,full;
  if(!(in>>pos>>turn>>cast>>ep>>half>>full))return std::nullopt;
  std::string extra; if(in>>extra)return std::nullopt;
  int row=0,col=0,wk=0,bk=0; bool digit=false;
  for(char p:pos){
    if(p=='/') {if(col!=8||row>=7)return std::nullopt;++row;col=0;digit=false;continue;}
    if(p>='1'&&p<='8'){if(digit)return std::nullopt;digit=true;col+=p-'0';if(col>8)return std::nullopt;continue;}
    digit=false;
    if(std::string_view("PNBRQKpnbrqk").find(p)==std::string_view::npos||col>=8)return std::nullopt;
    s.b_[row*16+col++]=p;wk+=p=='K';bk+=p=='k';
  }
  if(row!=7||col!=8||wk!=1||bk!=1||(turn!="w"&&turn!="b"))return std::nullopt;
  s.turn_=turn=="w"?1:-1; s.castle_=0;
  if(cast!="-") {
    for(char c:cast){int bit=c=='K'?1:c=='Q'?2:c=='k'?4:c=='q'?8:0;if(!bit||(s.castle_&bit))return std::nullopt;s.castle_|=bit;}
  }
  s.ep_=ep=="-"?-1:parse_square(ep);
  if(ep!="-" && (s.ep_<0||(ep[1]!='3'&&ep[1]!='6')))return std::nullopt;
  auto parse_int=[](const std::string& x,int& n) {if(x.empty()||x.find_first_not_of("0123456789")!=std::string::npos)return false;auto [ptr,ec]=std::from_chars(x.data(),x.data()+x.size(),n);return ec==std::errc{}&&ptr==x.data()+x.size()&&n<=1000000;};
  if(!parse_int(half,s.half_)||!parse_int(full,s.full_)||s.half_<0||s.full_<1)return std::nullopt;
  for(int i=0;i<128;i++)if(on(i)&&lower(s.b_[i])=='p'&&((i>>4)==0||(i>>4)==7))return std::nullopt;
  for(int who:{1,-1}) {
    int count=0,pawns=0;
    for(int i=0;i<128;++i)if(on(i)&&color(s.b_[i])==who){++count;pawns+=lower(s.b_[i])=='p';}
    if(count>16||pawns>8)return std::nullopt;
  }
  // The side that just moved cannot have left its own king attacked.
  if(s.in_check(-s.turn_))return std::nullopt;
  if(((s.castle_&3)&&s.b_[116]!='K')||((s.castle_&12)&&s.b_[4]!='k')||
     ((s.castle_&1)&&s.b_[119]!='R')||((s.castle_&2)&&s.b_[112]!='R')||
     ((s.castle_&4)&&s.b_[7]!='r')||((s.castle_&8)&&s.b_[0]!='r'))return std::nullopt;
  if(s.ep_>=0 && ((s.ep_>>4)!=(s.turn_==1?2:5)||s.b_[s.ep_]!='.'||
     s.b_[s.ep_+16*s.turn_]!=(s.turn_==1?'p':'P')||s.b_[s.ep_-16*s.turn_]!='.'||s.half_!=0))return std::nullopt;
  return s;
}
std::string Game::fen() const {
  std::string out;
  for(int r=0;r<8;r++){int empty=0;for(int c=0;c<8;c++){char p=b_[r*16+c];if(p=='.'){++empty;continue;}if(empty){out+=static_cast<char>('0'+empty);empty=0;}out+=p;}if(empty)out+=static_cast<char>('0'+empty);if(r!=7)out+='/';}
  out+=(turn_==1?" w ":" b ");
  if(castle_==0)out+="-";
  else{if(castle_&1)out+='K';if(castle_&2)out+='Q';if(castle_&4)out+='k';if(castle_&8)out+='q';}
  out+=' ';out+=(ep_<0?"-":square(ep_));out+=' ';out+=std::to_string(half_);out+=' ';out+=std::to_string(full_);return out;
}
bool Game::attacked(int target,int by) const {
  for(int f=0;f<128;++f){if(!on(f)||color(b_[f])!=by)continue;
    char p=lower(b_[f]);
    if(p=='p'){if(target==f-16*by-1||target==f-16*by+1)return true;continue;}
    const int *ds=nullptr;int count=0;
    if(p=='n'){ds=knight;count=8;}else if(p=='b'){ds=bishop;count=4;}else if(p=='r'){ds=rook;count=4;}else if(p=='q'||p=='k'){ds=king;count=8;}
    if(!ds)continue;
    for(int j=0;j<count;j++)for(int t=f+ds[j];on(t);t+=ds[j]){if(t==target)return true;if(b_[t]!='.'||p=='n'||p=='k')break;}
  }
  return false;
}
bool Game::in_check(int who) const {if(!who)who=turn_;int k=-1;for(int i=0;i<128;i++)if(on(i)&&b_[i]==(who==1?'K':'k')){k=i;break;}return k<0||attacked(k,-who);}
std::vector<Move> Game::pseudo() const {
  std::vector<Move> v;
  for(int f=0;f<128;f++){if(!on(f)||color(b_[f])!=turn_)continue;char p=lower(b_[f]);
    auto add=[&](int t,bool enp=false,bool castle=false){if(!on(t)||lower(b_[t])=='k')return;if(p=='p'&&((t>>4)==0||(t>>4)==7)){for(char c:std::string("qrbn"))v.push_back({f,t,c,enp,castle});}else v.push_back({f,t,0,enp,castle});};
    if(p=='p') {
      int t=f-16*turn_;
      if(on(t)&&b_[t]=='.'){add(t);int u=t-16*turn_;if((f>>4)==(turn_==1?6:1)&&on(u)&&b_[u]=='.')add(u);}
      for(int d:{-1,1}){int u=t+d;if(!on(u))continue;if(color(b_[u])==-turn_)add(u);else if(u==ep_&&b_[u]=='.'&&b_[u+16*turn_]==(turn_==1?'p':'P'))add(u,true);}
      continue;
    }
    const int* ds=nullptr;int count=0;
    if(p=='n'){ds=knight;count=8;}else if(p=='b'){ds=bishop;count=4;}else if(p=='r'){ds=rook;count=4;}else if(p=='q'||p=='k'){ds=king;count=8;}
    for(int j=0;j<count;j++)for(int t=f+ds[j];on(t);t+=ds[j]){if(color(b_[t])==turn_)break;add(t);if(b_[t]!='.'||p=='k'||p=='n')break;}
    if(p=='k'){
      int home=turn_==1?116:4;int r=turn_==1?'R':'r';int bk=turn_==1?1:4,bq=turn_==1?2:8;
      if(f==home&&!in_check()){
        if((castle_&bk)&&b_[f+3]==r&&b_[f+1]=='.'&&b_[f+2]=='.'&&!attacked(f+1,-turn_)&&!attacked(f+2,-turn_))add(f+2,false,true);
        if((castle_&bq)&&b_[f-4]==r&&b_[f-1]=='.'&&b_[f-2]=='.'&&b_[f-3]=='.'&&!attacked(f-1,-turn_)&&!attacked(f-2,-turn_))add(f-2,false,true);
      }
    }
  }
  return v;
}
Game Game::apply(Move m) const {
  Game s=*this;char p=s.b_[m.from], cap=s.b_[m.to];s.b_[m.from]='.';
  s.b_[m.to]=m.promotion?(turn_==1?static_cast<char>(std::toupper(m.promotion)):m.promotion):p;
  if(m.en_passant)s.b_[m.to+16*turn_]='.';
  if(m.castle){int rf=m.to>m.from?m.from+3:m.from-4,rt=m.to>m.from?m.from+1:m.from-1;s.b_[rt]=s.b_[rf];s.b_[rf]='.';}
  if(p=='K')s.castle_ &=~3;
  if(p=='k')s.castle_ &=~12;
  auto rook_bit=[](int i){return i==119?1:i==112?2:i==7?4:i==0?8:0;};
  if(lower(p)=='r')s.castle_&=~rook_bit(m.from);
  if(lower(cap)=='r')s.castle_&=~rook_bit(m.to);
  s.ep_=lower(p)=='p'&&std::abs(m.to-m.from)==32?(m.to+m.from)/2:-1;
  s.half_=lower(p)=='p'||cap!='.'||m.en_passant?0:std::min(half_+1,1000000);
  s.full_=std::min(full_+(turn_==-1),1000000);s.turn_=-turn_;return s;
}
std::vector<Move> Game::legal() const {std::vector<Move> v;for(auto m:pseudo())if(!apply(m).in_check(turn_))v.push_back(m);return v;}
std::string Game::uci(Move m){return square(m.from)+square(m.to)+(m.promotion?std::string(1,m.promotion):"");}
std::optional<Game> Game::play(std::string_view move) const {if(move.size()!=4&&move.size()!=5)return std::nullopt;for(auto m:legal())if(uci(m)==move)return apply(m);return std::nullopt;}
std::uint64_t Game::perft(int d) const {if(d<0||d>4)return 0;if(!d)return 1;auto moves=legal();if(d==1)return moves.size();std::uint64_t n=0;for(auto m:moves)n+=apply(m).perft(d-1);return n;}

std::string Game::position_key() const {
  Game normalized=*this;
  bool eligible=false;
  if(ep_>=0)for(auto m:legal())eligible=eligible||m.en_passant;
  if(!eligible)normalized.ep_=-1;
  std::string key=normalized.fen();
  return key.substr(0,key.rfind(' ',key.rfind(' ')-1));
}
std::string Game::san(std::string_view move) const {
  auto all=legal();
  for(auto m:all)if(uci(m)==move) {
    std::string out;
    char p=lower(b_[m.from]); bool capture=b_[m.to]!='.'||m.en_passant;
    if(m.castle)out=m.to>m.from?"O-O":"O-O-O";
    else {
      if(p!='p') {
        out+=static_cast<char>(std::toupper(static_cast<unsigned char>(p)));
        bool other=false,same_file=false,same_rank=false;
        for(auto n:all)if(n.from!=m.from&&n.to==m.to&&b_[n.from]==b_[m.from]) {
          other=true;same_file|=(n.from&7)==(m.from&7);same_rank|=(n.from>>4)==(m.from>>4);
        }
        if(other) {if(!same_file)out+=square(m.from)[0];else if(!same_rank)out+=square(m.from)[1];else out+=square(m.from);}
      } else if(capture)out+=square(m.from)[0];
      if(capture)out+='x';
      out+=square(m.to);
      if(m.promotion){out+='=';out+=static_cast<char>(std::toupper(static_cast<unsigned char>(m.promotion)));}
    }
    auto next=apply(m);
    if(next.in_check())out+=next.legal().empty()?'#':'+';
    return out;
  }
  return {};
}
bool Game::dead_position() const {
  int minors=0,knights=0,bishop_color=-1;bool mixed=false;
  for(int i=0;i<128;++i)if(on(i)&&b_[i]!='.') {
    char p=lower(b_[i]);if(p=='k')continue;
    if(p!='n'&&p!='b')return false;
    ++minors;if(p=='n')++knights;
    else {int c=((i>>4)+(i&7))&1;if(bishop_color>=0&&bishop_color!=c)mixed=true;bishop_color=c;}
  }
  // Sufficient cases only: KNN/K and opposite-color bishops are not dead.
  return minors<=1||(knights==0&&!mixed);
}
std::string Game::status() const {
  if(legal().empty())return in_check()?"CHECKMATE":"STALEMATE";
  if(dead_position())return "DRAW_DEAD";
  if(half_>=150)return "DRAW_75_MOVE";
  return in_check()?"CHECK":"ONGOING";
}
Session::Session(Game initial):states_{initial},keys_{initial.position_key()} {}
int Session::repetitions() const {return static_cast<int>(std::count(keys_.begin(),keys_.end(),keys_.back()));}
std::string Session::status() const {
  if(!claim_.empty())return "DRAW_CLAIMED";
  auto s=game().status();
  if(s!="ONGOING"&&s!="CHECK")return s;
  if(repetitions()>=5)return "DRAW_5_REPETITION";
  return s;
}
bool Session::play(std::string_view move) {
  auto s=status();if((s!="ONGOING"&&s!="CHECK")||moves_.size()>=max_plies)return false;
  auto next=game().play(move);if(!next)return false;
  // Commit only after all potentially throwing allocations have succeeded.
  Session copy=*this;
  copy.sans_.push_back(game().san(move));copy.moves_.emplace_back(move);
  copy.keys_.push_back(next->position_key());copy.states_.push_back(*next);
  *this=std::move(copy);return true;
}
bool Session::undo() {
  if(!claim_.empty()){claim_.clear();return true;}
  if(moves_.empty())return false;
  states_.pop_back();keys_.pop_back();moves_.pop_back();sans_.pop_back();return true;
}
bool Session::can_claim(std::string_view intended) const {
  auto s=status();if(s!="ONGOING"&&s!="CHECK")return false;
  if(intended.empty())return game().halfmove()>=100||repetitions()>=3;
  auto next=game().play(intended);if(!next)return false;
  auto key=next->position_key();
  return next->halfmove()>=100||std::count(keys_.begin(),keys_.end(),key)>=2;
}
bool Session::claim(std::string_view intended) {
  if(!can_claim(intended))return false;
  claim_=intended.empty()?"now":std::string(intended);return true;
}
std::string Session::save() const {
  std::string out="VKRT1\n"+states_.front().fen()+"\n";
  for(std::size_t i=0;i<moves_.size();++i){if(i)out+=' ';out+=moves_[i];}
  return out+"\n"+(claim_.empty()?"none":claim_)+"\n";
}
std::optional<Session> Session::load(std::string_view data) {
  if(data.size()>max_save_bytes)return std::nullopt;
  std::istringstream in{std::string(data)};std::string version,fen,moves,claim,extra;
  if(!std::getline(in,version)||version!="VKRT1"||!std::getline(in,fen)||!std::getline(in,moves)||!std::getline(in,claim)||std::getline(in,extra))return std::nullopt;
  auto initial=Game::from_fen(fen);if(!initial)return std::nullopt;
  Session s(*initial);std::istringstream list(moves);std::string move;
  while(list>>move)if(!s.play(move))return std::nullopt;
  if(claim.empty()||(claim!="none"&&!s.claim(claim=="now"?"":claim)))return std::nullopt;
  return s;
}
std::string Session::history() const {
  std::string out;
  for(std::size_t i=0;i<sans_.size();++i)out+=std::to_string(states_[i].fullmove())+(states_[i].turn()==1?". ":"... ")+sans_[i]+" ("+moves_[i]+")\n";
  return out;
}
std::string Session::pgn() const {
  std::string state=status(),result="*";
  if(state=="CHECKMATE")result=game().turn()==1?"0-1":"1-0";
  else if(state=="STALEMATE"||state.rfind("DRAW_",0)==0)result="1/2-1/2";
  std::string out="[Event \"Local game\"]\n[Site \"?\"]\n[Date \"????.??.??\"]\n[Round \"?\"]\n[White \"?\"]\n[Black \"?\"]\n[Result \""+result+"\"]\n";
  if(states_.front().fen()!=Game::start_fen)out+="[SetUp \"1\"]\n[FEN \""+states_.front().fen()+"\"]\n";
  out+='\n';
  for(std::size_t i=0;i<sans_.size();++i){if(states_[i].turn()==1)out+=std::to_string(states_[i].fullmove())+". ";else if(i==0)out+=std::to_string(states_[i].fullmove())+"... ";out+=sans_[i]+" ";}
  return out+result+"\n";
}
}
