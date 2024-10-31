/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Contains the pause, resume, and paused commands.
 * <p>
 * This thing is scoped to hell, private so far you can't even access it using reflection, because you AREN'T SUPPOSED
 * TO USE THIS to pause and resume Baritone. Make your own process that returns {@link PathingCommandType#REQUEST_PAUSE
 * REQUEST_PAUSE} as needed.
 */
public class ExecutionControlCommands {

    Command pauseCommand;
    Command resumeCommand;
    Command pausedCommand;
    Command cancelCommand;

    public ExecutionControlCommands(IBaritone baritone) {
        // array for mutability, non-field so reflection can't touch it
        final boolean[] paused = {false};
        baritone.getPathingControlManager().registerProcess(
                new IBaritoneProcess() {
                    @Override
                    public boolean isActive() {
                        return paused[0];
                    }

                    @Override
                    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
                        baritone.getInputOverrideHandler().clearAllKeys();
                        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                    }

                    @Override
                    public boolean isTemporary() {
                        return true;
                    }

                    @Override
                    public void onLostControl() {
                    }

                    @Override
                    public double priority() {
                        return DEFAULT_PRIORITY + 1;
                    }

                    @Override
                    public String displayName0() {
                        return "Pause/Resume Commands";
                    }
                }
        );
        pauseCommand = new Command(baritone, "pause", "p", "paws") {
            @Override
            public void execute(String label, IArgConsumer args) throws CommandException {
                args.requireMax(0);
                if (paused[0]) {
                    throw new CommandInvalidStateException("Already paused");
                }
                paused[0] = true;
                logDirect("Paused");
            }

            @Override
            public Stream<String> tabComplete(String label, IArgConsumer args) {
                return Stream.empty();
            }

            @Override
            public String getShortDesc() {
                return "暂停 Baritone ，直到你使用resume命令";
            }

            @Override
            public List<String> getLongDesc() {
                return Arrays.asList(
                        "这个 pause 命令 告诉 Baritone 暂时停止它正在做的事情.",
                        "",
                        "这可用于暂停路径、构建、跟踪等。只需使用一次resume命令，它就会重新启动！",
                        "",
                        "Usage:",
                        "> pause"
                );
            }
        };
        resumeCommand = new Command(baritone, "resume", "r", "unpause", "unpaws") {
            @Override
            public void execute(String label, IArgConsumer args) throws CommandException {
                args.requireMax(0);
                baritone.getBuilderProcess().resume();
                if (!paused[0]) {
                    throw new CommandInvalidStateException("Not paused");
                }
                paused[0] = false;
                logDirect("Resumed");
            }

            @Override
            public Stream<String> tabComplete(String label, IArgConsumer args) {
                return Stream.empty();
            }

            @Override
            public String getShortDesc() {
                return "恢复 Baritone 之前暂停的任务";
            }

            @Override
            public List<String> getLongDesc() {
                return Arrays.asList(
                        "这个 resume 命令 告诉 Baritone 继续你上次使用暂停时正在做的事情.",
                        "",
                        "Usage:",
                        "> resume"
                );
            }
        };
        pausedCommand = new Command(baritone, "paused") {
            @Override
            public void execute(String label, IArgConsumer args) throws CommandException {
                args.requireMax(0);
                logDirect(String.format("Baritone is %spaused", paused[0] ? "" : "not "));
            }

            @Override
            public Stream<String> tabComplete(String label, IArgConsumer args) {
                return Stream.empty();
            }

            @Override
            public String getShortDesc() {
                return "查询是否有任务被pause暂停";
            }

            @Override
            public List<String> getLongDesc() {
                return Arrays.asList(
                        "paused命令可查询是否有任务被pause暂停.",
                        "",
                        "Usage:",
                        "> paused"
                );
            }
        };
        cancelCommand = new Command(baritone, "cancel", "c", "stop") {
            @Override
            public void execute(String label, IArgConsumer args) throws CommandException {
                args.requireMax(0);
                if (paused[0]) {
                    paused[0] = false;
                }
                baritone.getPathingBehavior().cancelEverything();
                logDirect("ok canceled");
            }

            @Override
            public Stream<String> tabComplete(String label, IArgConsumer args) {
                return Stream.empty();
            }

            @Override
            public String getShortDesc() {
                return "取消Baritone当前正在进行的操作";
            }

            @Override
            public List<String> getLongDesc() {
                return Arrays.asList(
                        "cancel命令告诉Baritone停止它目前正在做的任何事情.",
                        "",
                        "Usage:",
                        "> cancel"
                );
            }
        };
    }
}
