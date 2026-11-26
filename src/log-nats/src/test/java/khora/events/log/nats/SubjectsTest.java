package khora.events.log.nats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import khora.events.EventId;
import org.junit.jupiter.api.Test;

class SubjectsTest {

    @Test
    void anEventIsFiledUnderItsIdAsOneToken() {
        var id = EventId.hashOf("x");
        var subject = Subjects.forEvent(id);
        assertTrue(subject.startsWith(Subjects.PREFIX), subject);
        var token = subject.substring(Subjects.PREFIX.length());
        assertTrue(token.matches("[A-Za-z0-9_-]+"), subject);
        assertEquals("khora.events.>", Subjects.ALL);
    }

    @Test
    void differentEventsHaveDifferentSubjects() {
        assertTrue(!Subjects.forEvent(EventId.hashOf("a")).equals(Subjects.forEvent(EventId.hashOf("b"))));
    }
}
