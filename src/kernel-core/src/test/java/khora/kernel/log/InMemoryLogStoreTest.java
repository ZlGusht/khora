package khora.kernel.log;

class InMemoryLogStoreTest extends LogStoreContract {
    @Override
    protected LogStore newStore() {
        return new InMemoryLogStore();
    }
}
