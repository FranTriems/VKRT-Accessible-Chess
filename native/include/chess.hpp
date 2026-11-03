#pragma once
#include <array>
#include <cstdint>
#include <optional>
#include <string>
#include <string_view>
#include <vector>

namespace vkrt::chess {
struct Move { int from{}, to{}; char promotion{}; bool en_passant{}, castle{}; };
class Game {
public:
  static constexpr std::string_view start_fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
  static std::optional<Game> from_fen(std::string_view fen);
  [[nodiscard]] std::string fen() const;
  [[nodiscard]] std::vector<Move> legal() const;
  [[nodiscard]] bool in_check(int color = 0) const;
  [[nodiscard]] std::optional<Game> play(std::string_view uci) const;
  [[nodiscard]] std::uint64_t perft(int depth) const;
  [[nodiscard]] std::string position_key() const;
  [[nodiscard]] std::string san(std::string_view uci) const;
  [[nodiscard]] bool dead_position() const;
  [[nodiscard]] std::string status() const;
  [[nodiscard]] int turn() const { return turn_; }
  [[nodiscard]] int halfmove() const { return half_; }
  [[nodiscard]] int fullmove() const { return full_; }
  [[nodiscard]] const std::array<char,128>& squares() const { return b_; }
  static std::string uci(Move m);
private:
  std::array<char,128> b_{};
  int turn_{1}, castle_{15}, ep_{-1}, half_{0}, full_{1};
  [[nodiscard]] bool attacked(int target, int by) const;
  [[nodiscard]] std::vector<Move> pseudo() const;
  [[nodiscard]] Game apply(Move m) const;
};

// Headless, bounded, replayable history. Game remains a value-only position.
class Session {
public:
  static constexpr std::size_t max_plies = 2048;
  static constexpr std::size_t max_save_bytes = 16384;
  explicit Session(Game initial);
  static std::optional<Session> load(std::string_view data);
  [[nodiscard]] const Game& game() const { return states_.back(); }
  [[nodiscard]] std::string save() const;
  [[nodiscard]] std::string status() const;
  [[nodiscard]] std::string history() const;
  [[nodiscard]] std::string pgn() const;
  [[nodiscard]] int repetitions() const;
  [[nodiscard]] bool can_claim(std::string_view intended = {}) const;
  bool play(std::string_view move);
  bool undo();
  bool claim(std::string_view intended = {});
private:
  std::vector<Game> states_;
  std::vector<std::string> keys_, moves_, sans_;
  std::string claim_; // "now" or an announced move which has NOT been played.
};
}
