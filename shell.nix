
{ jdk ? "jdk21" }:

let
  pkgs = import nix/pkgs.nix { inherit jdk; };
in
  pkgs.mkShell {
    buildInputs = [
      pkgs.${jdk}
      pkgs.sbt
    ];
  }
