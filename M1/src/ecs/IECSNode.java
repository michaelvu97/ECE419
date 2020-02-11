package ecs;

import shared.metadata.*;

public interface IECSNode {

    /**
     * @return  the name of the node (ie "Server 8.8.8.8")
     */
    public String getNodeName();

    /**
     * @return  the hostname of the node (ie "8.8.8.8")
     */
    public String getNodeHost();

    /**
     * @return  the port number of the node (ie 8080)
     */
    public int getNodePort();

    /**
     * @return  array of two strings representing the low and high range of the hashes that the given node is responsible for
     */
    public String[] getNodeHashRange();

    public void setFlag(ECSNodeFlag flag);
    public ECSNodeFlag getFlag();

    public MetaData getMetaData();

    public enum ECSNodeFlag {
        STOP,              /* Node has stopped */
        START,             /* Node has started */
        STATE_CHANGE,      /* Node state has changed*/
        KV_TRANSFER,       /* Data transfer occurred */
        SHUT_DOWN,         /* Node has shutdown*/
        UPDATE,            /* Node has updated */
        TRANSFER_FINISH    /* Data transfer operation finished */
    }
}
