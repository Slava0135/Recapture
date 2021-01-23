import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.game.Teams.*;
import mindustry.gen.Call;
import mindustry.mod.Plugin;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Recapture extends Plugin {
    final float distance = 80;
    HashMap<Point2, Integer> underContest = new HashMap<>();

    @Override
    public void init() {
        Timer.schedule(() -> {

            Seq<TeamData> teams = Vars.state.teams.present;
            for (TeamData team : teams) {
                for (CoreBuild core : team.cores) {
                    boolean[] contested = {false};
                    for (TeamData anotherTeam : teams) {
                        if(anotherTeam.team == team.team) continue;
                        Units.nearby(anotherTeam.team, core.x - distance, core.y - distance, 2 * distance, 2 * distance, u -> {
                            if (!u.spawnedByCore) {
                                contested[0] = true;
                            }
                        });
                    }
                    if (contested[0]) {
                        Point2 point = new Point2(core.tile.x, core.tile.y);
                        underContest.putIfAbsent(point, 0);
                    }
                }
            }

            for (Iterator<Map.Entry<Point2, Integer>> it = underContest.entrySet().iterator(); it.hasNext(); ) {

                Map.Entry<Point2, Integer> entry = it.next();

                Point2 point = entry.getKey();
                int progress = entry.getValue();

                Tile tile = Vars.world.tileBuilding(point.x, point.y);
                if (tile == null || !(tile.build instanceof CoreBuild)) {
                    it.remove();
                    continue;
                }

                CoreBuild core = (CoreBuild) tile.build;

                AtomicReference<Team> firstTeam = new AtomicReference<>();
                boolean[] contested = {false};
                boolean[] inProgress = {true};
                Units.nearbyEnemies(tile.team(), core.x - distance, core.y - distance, 2 * distance, 2 * distance, u -> {
                    if (!u.spawnedByCore) contested[0] = true;
                    if (firstTeam.get() == null) {
                        firstTeam.set(u.team);
                    } else {
                        if (firstTeam.get() != u.team && tile.team() == Team.derelict) { //more than one team present nearby
                            inProgress[0] = false;
                        }
                    }
                });

                boolean[] hold = {false};
                Units.nearby(core.team, core.x - distance, core.y - distance, 2 * distance, 2 * distance, u -> {
                    if (!u.spawnedByCore) {
                        hold[0] = true;
                    }
                });

                int newProgress = progress;
                if (!contested[0]) {
                    newProgress -= 5; //no enemies nearby
                } else if (inProgress[0] && !hold[0]) {
                    newProgress += 5; //no allies nearby
                }

                if (newProgress <= 0) {
                    it.remove();
                } else if (newProgress >= 100){
                    it.remove();
                    if (core.team != Team.derelict) {
                        captureCore((CoreBuild) tile.build, Team.derelict);
                    } else {
                        captureCore((CoreBuild) tile.build, firstTeam.get());
                    }
                } else {
                    underContest.put(point, newProgress);
                    Call.effectReliable(Fx.placeBlock, core.x, core.y, distance / 4, core.team.color);
                    Call.label(String.valueOf(newProgress), 0.5f, core.x, core.y);
                }
            }

        }, 0, 0.5f);
    }

    void captureCore(CoreBuild core, Team team) {
        Call.effectReliable(Fx.upgradeCore, core.x, core.y, core.block.size, team.color);
        if (team != Team.derelict) Call.label("Captured!", 1, core.x, core.y);
        core.tile.setNet(core.block, team, 0);
    }
}
