package khora.surfaces.stub;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import khora.events.Action;
import khora.events.Basic;
import khora.events.Emitted;
import khora.events.Event;
import khora.events.Statement;
import khora.events.Statements;
import khora.surfaces.Interest;
import khora.surfaces.Surface;

/**
 * A reference surface for entities: it takes in every assert, and answers each read with the
 * statements that stand for it, as {@link Statements} reads them. Its answer names the read as its
 * trigger.
 *
 * <p>Exploration: this is one reading of the events. Two replacements of one statement both stand
 * here; a gate, or another surface, may read them otherwise.
 */
public final class Entities implements Surface {
    private final List<Event> asserts = new ArrayList<>();

    @Override
    public String name() {
        return "entities";
    }

    @Override
    public Set<Interest> interests() {
        return Set.of(new Interest(Set.of(Basic.ASSERT, Basic.READ), Set.of()));
    }

    @Override
    public List<Action> receive(Event event) {
        var action = ((Emitted) event.body()).action();
        var read = Basic.readOf(action);
        if (read.isEmpty()) {
            asserts.add(event);
            return List.of();
        }
        var held = Statements.about(read.get(), asserts).stream().map(Statement::id).toList();
        return List.of(Basic.answer(read.get(), held));
    }
}
