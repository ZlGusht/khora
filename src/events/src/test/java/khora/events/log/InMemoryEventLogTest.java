package khora.events.log;

class InMemoryEventLogTest extends EventLogContract {
    @Override
    protected EventLog open() {
        return new InMemoryEventLog();
    }
}
