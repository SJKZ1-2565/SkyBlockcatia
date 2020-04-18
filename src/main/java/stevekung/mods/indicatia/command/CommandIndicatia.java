package stevekung.mods.indicatia.command;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.BlockPos;
import stevekung.mods.indicatia.config.ExtendedConfig;
import stevekung.mods.indicatia.gui.config.GuiExtendedConfig;
import stevekung.mods.indicatia.utils.JsonUtils;
import stevekung.mods.indicatia.utils.LangUtils;

public class CommandIndicatia extends ClientCommandBase
{
    @Override
    public String getCommandName()
    {
        return "indicatia";
    }

    @Override
    public List<String> getCommandAliases()
    {
        return Arrays.asList("in");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 1)
        {
            throw new WrongUsageException("commands.indicatia.usage");
        }
        else
        {
            if ("toggle_sprint".equalsIgnoreCase(args[0]))
            {
                if (args.length == 1)
                {
                    throw new WrongUsageException("commands.indicatia.togglesprint.usage");
                }

                if ("enable".equalsIgnoreCase(args[1]))
                {
                    ExtendedConfig.instance.toggleSprint = true;
                    sender.addChatMessage(JsonUtils.create(LangUtils.translate("message.toggle_sprint_enabled")));
                    ExtendedConfig.instance.save();
                }
                else if ("disable".equalsIgnoreCase(args[1]))
                {
                    ExtendedConfig.instance.toggleSprint = false;
                    sender.addChatMessage(JsonUtils.create(LangUtils.translate("message.toggle_sprint_disabled")));
                    ExtendedConfig.instance.save();
                }
                else if ("mode".equalsIgnoreCase(args[1]))
                {
                    if (args.length < 3 || args.length > 3)
                    {
                        throw new WrongUsageException("commands.indicatia.togglesprint.mode.usage");
                    }

                    if ("key_binding".equalsIgnoreCase(args[2]))
                    {
                        ExtendedConfig.instance.toggleSprintUseMode = "key_binding";
                        sender.addChatMessage(JsonUtils.create(LangUtils.translate("message.toggle_sprint_set") + LangUtils.translate("message.key_binding")));
                        ExtendedConfig.instance.save();
                    }
                    else if ("command".equalsIgnoreCase(args[2]))
                    {
                        ExtendedConfig.instance.toggleSprintUseMode = "command";
                        sender.addChatMessage(JsonUtils.create(LangUtils.translate("message.toggle_sprint_set") + LangUtils.translate("message.command")));
                        ExtendedConfig.instance.save();
                    }
                    else
                    {
                        throw new WrongUsageException("commands.indicatia.togglesprint.mode.usage");
                    }
                }
                else
                {
                    throw new WrongUsageException("commands.indicatia.togglesprint.usage");
                }
            }
            else if ("toggle_sneak".equalsIgnoreCase(args[0]))
            {
                if (args.length == 1)
                {
                    throw new WrongUsageException("commands.indicatia.togglesneak.usage");
                }

                if ("enable".equalsIgnoreCase(args[1]))
                {
                    ExtendedConfig.instance.toggleSneak = true;
                    sender.addChatMessage(JsonUtils.create(LangUtils.translate("message.toggle_sneak_enabled")));
                    ExtendedConfig.instance.save();
                }
                else if ("disable".equalsIgnoreCase(args[1]))
                {
                    ExtendedConfig.instance.toggleSneak = false;
                    sender.addChatMessage(JsonUtils.create(LangUtils.translate("message.toggle_sneak_disabled")));
                    ExtendedConfig.instance.save();
                }
                else if ("mode".equalsIgnoreCase(args[1]))
                {
                    if (args.length < 3 || args.length > 3)
                    {
                        throw new WrongUsageException(LangUtils.translate("message.toggle_sneak_set") + LangUtils.translate("message.key_binding"));
                    }
                    if ("key_binding".equalsIgnoreCase(args[2]))
                    {
                        ExtendedConfig.instance.toggleSneakUseMode = "key_binding";
                        sender.addChatMessage(JsonUtils.create("Set toggle sneak to use Key Binding"));
                        ExtendedConfig.instance.save();
                    }
                    else if ("command".equalsIgnoreCase(args[2]))
                    {
                        ExtendedConfig.instance.toggleSneakUseMode = "command";
                        sender.addChatMessage(JsonUtils.create(LangUtils.translate("message.toggle_sneak_set") + LangUtils.translate("message.command")));
                        ExtendedConfig.instance.save();
                    }
                    else
                    {
                        throw new WrongUsageException("commands.indicatia.togglesneak.mode.usage");
                    }
                }
                else
                {
                    throw new WrongUsageException("commands.indicatia.togglesneak.usage");
                }
            }
            else if ("gui".equalsIgnoreCase(args[0]))
            {
                GuiExtendedConfig options = new GuiExtendedConfig();
                options.display();
            }
            else
            {
                throw new WrongUsageException("commands.indicatia.usage");
            }
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos)
    {
        if (args.length == 1)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "toggle_sprint", "toggle_sneak", "gui");
        }
        else if (args.length == 2)
        {
            if (args[0].equalsIgnoreCase("toggle_sprint") || args[0].equalsIgnoreCase("toggle_sneak"))
            {
                return CommandBase.getListOfStringsMatchingLastWord(args, "enable", "disable", "mode");
            }
        }
        else if (args.length == 3)
        {
            if ((args[0].equalsIgnoreCase("toggle_sprint") || args[0].equalsIgnoreCase("toggle_sneak")) && args[1].equalsIgnoreCase("mode"))
            {
                return CommandBase.getListOfStringsMatchingLastWord(args, "key_binding", "command");
            }
        }
        return super.addTabCompletionOptions(sender, args, pos);
    }
}