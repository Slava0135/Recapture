import arc.math.geom.Point2;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.mod.Plugin;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Recapture extends Plugin {
    final float distance = 64;
    HashMap<Point2, Integer> underContest = new HashMap<>();

    @Override
    public void init() {
        Timer.schedule(() -> {
            for (var team : Vars.state.teams.active) {
                for (var core : team.cores) {
                    var enemy = Units.closestEnemy(team.team, core.x, core.y, distance, unit -> true);
                    if (enemy != null) {
                        var point = new Point2(core.tile.x, core.tile.y);
                        underContest.putIfAbsent(point, 0);
                    }
                }
            }
            for (Iterator<Map.Entry<Point2, Integer>> it = underContest.entrySet().iterator(); it.hasNext(); ) {

                Map.Entry<Point2, Integer> entry = it.next();

                var point = entry.getKey();
                var progress = entry.getValue();

                var tile = Vars.world.tileBuilding(point.x, point.y);
                if (tile == null || !(tile.build instanceof CoreBuild)) {
                    it.remove();
                    continue;
                }

                AtomicReference<Team> firstTeam = new AtomicReference<>();
                boolean[] contested = {false};
                boolean[] inProgress = {true};
                Units.nearbyEnemies(tile.team(), tile.worldx() - distance, tile.worldy() - distance, 2 * distance, 2 * distance, u -> {
                    contested[0] = true;
                    if (firstTeam.get() == null) {
                        firstTeam.set(u.team);
                    } else {
                        if (firstTeam.get() != u.team && tile.team() == Team.derelict) { //more than one team present nearby
                            inProgress[0] = false;
                        }
                    }
                });

                var newProgress = 0;
                if (!contested[0]) {
                    newProgress = progress - 10; //no enemies nearby
                } else if (inProgress[0] && Units.closest(tile.team(), tile.worldx(), tile.worldy(), distance, u -> true) == null) {
                    newProgress = progress + 10; //no allies nearby
                }

                Log.info(progress);

                if (newProgress <= 0) {
                    it.remove();
                } else if (newProgress >= 100){
                    it.remove();
                    captureCore((CoreBuild) tile.build, firstTeam.get());
                } else {
                    underContest.put(point, newProgress);
                }
            }
        }, 0, 1f);
    }

    void captureCore(CoreBuild core, Team team) {
        Call.effectReliable(Fx.upgradeCore, core.x, core.y, core.block.size, team.color);
        core.tile.setNet(core.block, team, 0);
    }
}
