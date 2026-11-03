#include "chess.hpp"
#include <cstdint>
#include <iostream>
#include <vector>
using vkrt::chess::Game;
struct Case{const char* fen;int d;std::uint64_t expected;};
int main(){
 const std::vector<Case> v={
 {"8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",3,2812},
 {"r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",3,9467},
 {"rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",3,62379},
 {"r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",3,89890}
 };
 for(auto& c:v){auto g=Game::from_fen(c.fen);if(!g){std::cerr<<"BAD FEN\n";return 1;}auto n=g->perft(c.d);if(n!=c.expected){std::cerr<<"PERFT BAD "<<n<<" expected "<<c.expected<<"\n";return 2;}std::cout<<"PASS perft "<<n<<"\n";}
}
