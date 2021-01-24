import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Align;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class Recapture extends Plugin {
    float distance = -1;
    float baseDelta = -1;
    float sizeMultiplier = -1f;

    HashMap<Point2, Float> underContest = new HashMap<>();

    @Override
    public void init() {

        //load config
        Properties props = new Properties();
        try(InputStream resourceStream = Recapture.class.getResourceAsStream("config.properties")) {
            props.load(resourceStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        distance = Float.parseFloat(props.getProperty("distance"));
        baseDelta = Float.parseFloat(props.getProperty("baseDelta"));
        sizeMultiplier = Float.parseFloat(props.getProperty("sizeMultiplier"));

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
                        underContest.putIfAbsent(point, 0f);
                    }
                }
            }

            for (Iterator<Map.Entry<Point2, Float>> it = underContest.entrySet().iterator(); it.hasNext(); ) {

                Map.Entry<Point2, Float> entry = it.next();

                Point2 point = entry.getKey();
                float progress = entry.getValue();

                Tile tile = Vars.world.tileBuilding(point.x, point.y);
                if (tile == null || !(tile.build instanceof CoreBuild)) {
                    it.remove();
                    continue;
                }

                CoreBuild core = (CoreBuild) tile.build;

                AtomicReference<Team> firstTeam = new AtomicReference<>();
                boolean[] inProgress = {true};

                float[] delta = {baseDelta};
                Units.nearbyEnemies(tile.team(), core.x - distance, core.y - distance, 2 * distance, 2 * distance, u -> {
                    if (!u.spawnedByCore) {
                        if (firstTeam.get() == null) {
                            firstTeam.set(u.team);
                        } else {
                            if (firstTeam.get() != u.team && tile.team() == Team.derelict) { //more than one team present nearby
                                inProgress[0] = false;
                            } else if (inProgress[0]) {
                                delta[0] += sizeMultiplier * u.hitSize;
                            }
                        }
                    }
                });

                float newProgress = progress;
                if (inProgress[0]) {
                    Units.nearby(tile.team(), core.x - distance, core.y - distance, 2 * distance, 2 * distance, u -> {
                        if (!u.spawnedByCore) {
                            delta[0] -= sizeMultiplier * u.hitSize;
                        }
                    });
                    newProgress += delta[0];
                }

                if (newProgress <= 0) {
                    Call.label("0", 0.5f, core.x, core.y);
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
                    Call.label(String.valueOf((int) newProgress), 0.5f, core.x, core.y);
                }
            }

        }, 0, 0.5f);
    }

    void captureCore(CoreBuild core, Team team) {
        Call.effectReliable(Fx.upgradeCore, core.x, core.y, core.block.size, team.color);
        if (team != Team.derelict) {
            Call.label("[#" + team.color.toString() + "]Captured![]", 1, core.x, core.y);
            Call.infoPopup(
                    "Team [#" + team.color.toString() + "]" + team.name + " []captured " + "core at " + core.tile.x + ", " + core.tile.y
                    , 5f, Align.center, 50, 0, 0, 0);
        } else {
            Call.label("[#" + core.team.color.toString() + "]Lost![]", 1, core.x, core.y);
            Call.infoPopup(
                    "Team [#" + core.team.color.toString() + "]" + team.name + " []lost " + "core at " + core.tile.x + ", " + core.tile.y
                    , 5f, Align.center, 0, 0, 50, 0);
        }
        core.tile.setNet(core.block, team, 0);
    }
}
