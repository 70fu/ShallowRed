package at.pwd.shallowred.CLI;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.*;


import java.util.List;
import java.util.concurrent.Callable;

@Command(name="ShallowRedCLI",subcommands={PlayGameCommand.class})
public class ShallowRedCLI implements Callable<Void>
{
    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new ShallowRedCLI());
        commandLine.setTrimQuotes(true);
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);

        //sub-commands are registered declaratively via annotations

        List<Object> result = commandLine.parseWithHandler(new RunLast(),args);
    }

    @Override
    public Void call() throws Exception
    {
        CommandLine.usage(new ShallowRedCLI(),System.out);
        return null;
    }
}
