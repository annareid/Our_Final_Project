//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

public class TCB {
    private Thread thread = null;
    private int tid = 0;
    private int pid = 0;
    private boolean terminated = false;
    private int sleepTime = 0;
    public FileTableEntry[] ftEnt = null;

    public TCB(Thread var1, int var2, int var3) {
        this.thread = var1;
        this.tid = var2;
        this.pid = var3;
        this.terminated = false;
        this.ftEnt = new FileTableEntry[32];
        System.err.println("threadOS: a new thread (thread=" + this.thread + " tid=" + this.tid + " pid=" + this.pid + ")");
    }

    public synchronized Thread getThread() {
        return this.thread;
    }

    public synchronized int getTid() {
        return this.tid;
    }

    public synchronized int getPid() {
        return this.pid;
    }

    public synchronized boolean setTerminated() {
        this.terminated = true;
        return this.terminated;
    }

    public synchronized boolean getTerminated() {
        return this.terminated;
    }

    public synchronized int getFd(FileTableEntry var1) {
        if(var1 == null) {
            return -1;
        } else {
            for(int var2 = 3; var2 < 32; ++var2) {
                if(this.ftEnt[var2] == null) {
                    this.ftEnt[var2] = var1;
                    return var2;
                }
            }

            return -1;
        }
    }

    public synchronized FileTableEntry returnFd(int var1) {
        if(var1 >= 3 && var1 < 32) {
            FileTableEntry var2 = this.ftEnt[var1];
            this.ftEnt[var1] = null;
            return var2;
        } else {
            return null;
        }
    }

    public synchronized FileTableEntry getFtEnt(int var1) {
        return var1 >= 3 && var1 < 32?this.ftEnt[var1]:null;
    }
}
