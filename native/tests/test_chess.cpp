#include "chess.hpp"
#include <iostream>
#include <cstdlib>
using vkrt::chess::Game;
static void verify(bool ok,const char* msg){if(!ok){std::cerr<<"FAIL "<<msg<<"\n";std::exit(1);}std::cout<<"PASS "<<msg<<"\n";}

using vkrt::chess::Session;
static Game position(const char* fen) {auto g=Game::from_fen(fen);verify(g.has_value(),"fixture valid");return *g;}
static const Game initial=*Game::from_fen(Game::start_fen);
static void cycle(Session& s) {for(const char* m:{"g1f3","g8f6","f3g1","f6g8"})verify(s.play(m),"repetition move");}
static void extra_tests(const std::string& suite) {
 if(suite=="special") {
  auto c=position("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
  verify(c.play("e1g1")&&c.play("e1c1"),"both castles");
  verify(c.play("e1g1")->fen()=="r3k2r/8/8/8/8/8/8/R4RK1 b kq - 1 1","castle rook and rights");
  verify(!position("r3k2r/8/8/8/2b5/8/8/R3K2R w KQkq - 0 1").play("e1g1"),"no castle through attack");
  auto p=position("7k/P7/8/8/8/8/8/4K3 w - - 0 1");
  for(const char* m:{"a7a8q","a7a8r","a7a8b","a7a8n"})verify(p.play(m).has_value(),"four promotions");
  verify(!p.play("a7a8"),"promotion choice required");
  auto ep=position("7k/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
  verify(ep.play("e5d6")->fen()=="7k/8/3P4/8/8/8/8/4K3 b - - 0 1","en passant removes pawn");
  verify(!position("k3r3/8/8/3pP3/8/8/8/4K3 w - d6 0 1").play("e5d6"),"pinned en passant illegal");
 } else if(suite=="fen") {
  for(const char* f:{"","8/8/8/8/8/8/8/8 w - - 0 1",
   "7k/8/8/8/8/8/8/4K3 w K - 0 1","7k/8/8/8/8/8/8/4K3 w - d6 0 1",
   "7k/8/8/8/8/8/8/4K3 w - - 2147483647 1","7k/8/8/8/8/8/8/4K3 w - - -1 1",
   "8/8/8/8/8/8/4k3/4K3 w - - 0 1","7k/8/8/8/8/8/8/4K2R w - - 0 1",
   "7k/8/8/8/8/8/8/4K111 w - - 0 1","7k/8/8/3pP3/8/8/8/4K3 b - d6 0 1",
   "7k/8/8/3pP3/8/8/8/4K3 w - d6 1 1"})
   verify(!Game::from_fen(f),"reject invalid FEN");
  verify(!Game::from_fen(std::string(257,'x')),"FEN size bound");
  auto g=position("7k/8/8/8/8/8/8/R3K3 b - - 1000000 1000000");
  verify(g.play("h8g8")->fullmove()==1000000,"counter saturation");
  std::uint32_t seed=0x7615u;const std::string alphabet="12345678/PNBRQKpnbrqk wbKQkq-09! \n";
  for(int n=0;n<2000;++n) {
   seed^=seed<<13;seed^=seed>>17;seed^=seed<<5;
   std::string input;auto len=seed%300;
   for(unsigned i=0;i<len;++i){seed=1664525u*seed+1013904223u;input+=alphabet[seed%alphabet.size()];}
   auto parsed=Game::from_fen(input);
   if(parsed)verify(Game::from_fen(parsed->fen()).has_value(),"fuzz roundtrip");
   if(initial.play(input))verify(false,"fuzz illegal move rejected");
  }
  verify(true,"2000 deterministic malformed input probes");
 } else if(suite=="repetition") {
  verify(position("7k/8/8/3p4/8/8/8/4K3 w - d6 0 1").position_key()==position("7k/8/8/3p4/8/8/8/4K3 w - - 7 5").position_key(),"irrelevant ep and counters ignored");
  verify(position("k3r3/8/8/3pP3/8/8/8/4K3 w - d6 0 1").position_key()==position("k3r3/8/8/3pP3/8/8/8/4K3 w - - 0 1").position_key(),"pinned ep ignored");
  verify(position("7k/8/8/3pP3/8/8/8/4K3 w - d6 0 1").position_key()!=position("7k/8/8/3pP3/8/8/8/4K3 w - - 0 1").position_key(),"legal ep distinguished");
  verify(position("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1").position_key()!=position("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1").position_key(),"rights distinguished");
  Session s(initial);cycle(s);cycle(s);
  verify(s.repetitions()==3&&s.can_claim()&&s.status()=="ONGOING","threefold claim only");
  auto saved=s.save();verify(s.undo()&&s.can_claim("f6g8"),"intended third repetition");
  verify(s.claim("f6g8")&&s.status()=="DRAW_CLAIMED"&&s.game().turn()==-1,"intended claim does not play");
  verify(s.undo()&&s.play("f6g8")&&s.save()==saved,"claim undo and replay exact");
  cycle(s);cycle(s);verify(s.status()=="DRAW_5_REPETITION"&&!s.play("e2e4"),"fivefold terminal");
 } else if(suite=="draws") {
  Session s(position("7k/8/8/8/8/8/8/R3K3 w - - 99 1"));
  verify(!s.can_claim()&&s.can_claim("a1a2"),"50 move intended claim");
  verify(s.play("a1a2")&&s.can_claim()&&s.claim(),"50 move current claim");
  verify(!s.play("h8g8")&&s.undo()&&s.can_claim(),"undo claim result");
  verify(position("7k/8/8/8/8/8/8/R3K3 w - - 150 1").status()=="DRAW_75_MOVE","75 automatic");
  verify(position("7k/5K2/6Q1/8/8/8/8/8 w - - 149 1").play("g6g7")->status()=="CHECKMATE","mate precedes 75 rule");
  verify(position("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1").status()=="STALEMATE","stalemate");
  for(const char* f:{"7k/8/8/8/8/8/8/4K3 w - - 0 1","7k/8/8/8/8/8/8/2B1K3 w - - 0 1","7k/8/8/8/8/8/8/1N2K3 w - - 0 1","5b1k/8/8/8/8/8/8/2B1K3 w - - 0 1"})
   verify(position(f).dead_position(),"sufficient dead material");
  verify(!position("7k/8/8/8/8/8/8/1N2KN2 w - - 0 1").dead_position(),"KNN not dead");
  verify(!position("2b4k/8/8/8/8/8/8/2B1K3 w - - 0 1").dead_position(),"opposite bishops not dead");
 } else if(suite=="san") {
  verify(initial.san("e2e4")=="e4","pawn SAN");
  verify(position("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1").san("e1c1")=="O-O-O","castle SAN");
  verify(position("7k/8/8/3pP3/8/8/8/4K3 w - d6 0 1").san("e5d6")=="exd6","ep SAN");
  verify(position("7k/P7/8/8/8/8/8/4K3 w - - 0 1").san("a7a8q")=="a8=Q+","promotion check SAN");
  verify(position("7k/8/8/8/8/8/8/1N2KN2 w - - 0 1").san("b1d2")=="Nbd2","SAN file");
  verify(position("7k/8/8/8/8/1N6/8/1N2K3 w - - 0 1").san("b1d2")=="N1d2","SAN rank");
  verify(position("7k/8/8/8/8/1N6/8/1N2KN2 w - - 0 1").san("b1d2")=="Nb1d2","SAN both");
  Session s(initial);for(const char* m:{"f2f3","e7e5","g2g4","d8h4"})verify(s.play(m),"fools mate");
  verify(s.pgn().find("1. f3 e5 2. g4 Qh4# 0-1")!=std::string::npos,"PGN mate result");
 } else if(suite=="session") {
  Session s(initial);auto before=s.save();verify(!s.play("e2e5")&&s.save()==before,"illegal move atomic");
  verify(s.play("e2e4")&&s.play("c7c5"),"session legal moves");
  auto loaded=Session::load(s.save());verify(loaded&&loaded->save()==s.save(),"save replay roundtrip");
  verify(loaded->undo()&&loaded->undo()&&loaded->save()==before,"full undo roundtrip");
  verify(!Session::load(s.save()+"junk")&&!Session::load(s.save().substr(0,15)),"corrupt save rejected");
  verify(!Session::load(std::string(16385,'x')),"save size bound");
  verify(!Session::load("VKRT1\n"+std::string(Game::start_fen)+"\ne2e5\nnone\n"),"illegal saved history rejected");
  Session custom(position("7k/8/8/8/8/8/8/R3K3 b - - 0 18"));
  verify(custom.play("h8g8")&&custom.pgn().find("18... Kg8 *")!=std::string::npos,"custom black numbering");
  verify(custom.pgn().find("[SetUp \"1\"]")!=std::string::npos,"PGN FEN header");
  verify(!Session::load("VKRT1\n7k/8/8/8/8/8/8/R3K3 w - - 100 1\n\n\n"),"empty claim field rejected");
  Session claimed(position("7k/8/8/8/8/8/8/R3K3 w - - 100 1"));
  verify(claimed.claim()&&Session::load(claimed.save())->status()=="DRAW_CLAIMED","claimed save replay");
 } else verify(false,"known suite");
}

int main(int argc,char** argv){
 if(argc==2){extra_tests(argv[1]);return 0;}
 auto start=Game::from_fen(Game::start_fen);verify(start.has_value(),"start parse");
 verify(start->fen()==Game::start_fen,"start fen roundtrip");
 for(auto [d,n]: {std::pair{1,20}, {2,400}, {3,8902}, {4,197281}})verify(start->perft(d)==static_cast<unsigned>(n),"start perft");
 verify(!start->play("e2e5"),"illegal move rejected");
 auto e4=start->play("e2e4");verify(e4.has_value(),"legal move e2e4");verify(e4->turn()==-1,"turn switches");verify(e4->fen().find(" e3 ")!=std::string::npos,"en passant target");
 auto k=Game::from_fen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
 verify(k.has_value(),"kiwipete parse");
 for(auto [d,n]:{std::pair{1,48},{2,2039},{3,97862}})verify(k->perft(d)==static_cast<unsigned>(n),"kiwipete perft");
 auto p=Game::from_fen("8/P7/7k/8/8/8/8/4K3 w - - 0 1");verify(p&&p->play("a7a8n").has_value(),"underpromotion");
 auto invalid=Game::from_fen("this is not fen");verify(!invalid,"reject malformed fen");
}
