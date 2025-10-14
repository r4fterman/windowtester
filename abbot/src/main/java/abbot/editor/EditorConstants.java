package abbot.editor;

/**
 * Provide Editor action key names and menu keys. Action names are looked up via {@link abbot.i18n.Strings#get(String)}
 * by prepending the string <code>"actions."</code>. NOTE: to add a new editor action, define a key for it here and an
 * action for it in ScriptEditor.  Add to the action map in ScriptEditor.initActions, then (optionally) add it to the
 * menu layout in ScriptEditor.initMenus.
 */
public interface EditorConstants {

  String ACTION_PREFIX = "actions.";
  String ACTION_TOGGLE_FORKED = "toggle-forked";
}
