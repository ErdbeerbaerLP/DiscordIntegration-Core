package de.erdbeerbaerlp.dcintegration.common.storage.configCmd;


public class ConfigCommand {
    public String name = "missingno", description = "No description provided.", mcCommand = "?";
    public boolean adminOnly = false, hidden = false;
    public CommandArgument[] args = new CommandArgument[0];

    public static class CommandArgument {
        public CommandArgument(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public CommandArgument(String name, String description, boolean optional) {
            this(name, description);
            this.optional = optional;
        }

        public String name = "missingno", description = "No description provided.";
        public boolean optional = false;
    }
}
