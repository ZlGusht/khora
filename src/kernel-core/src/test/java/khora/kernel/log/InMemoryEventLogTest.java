package khora.kernel.log;

class InMemoryEventLogTest extends EventLogContract {
    @Override
    protected EventLog open() {
        return new InMemoryEventLog();
    }
}
