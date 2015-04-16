import java.io.Serializable;

public class Request implements Serializable {
    public Request (Cloud.FrontEndOps.Request _rhs, long _rhs_time) {
	timeArrived = _rhs_time;
	_r = _rhs;
    }

    public long timeArrived = 0;
    public Cloud.FrontEndOps.Request _r = null;
}

