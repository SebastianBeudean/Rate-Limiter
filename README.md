# Rate limiter
The rate limiter algorithm I chose for the MQR engineering take-home assignment is **Sliding window log**, pulled from "*System Design Interview - An insider's guide*" by Alex Xu.
### Algorithm explanation
- a new request is read.
- check for outdated timestamp entries in the log:
	- an outdated timestamp entry is defined by the formula `timestampEntry < cutoff`, where `cutoff = latestTimestamp - windowSeconds`.
	- remove each outdated timestamp entry found.
- check if the size of the log is less than the `limit` of requests per `windowSeconds`:
	- if true, accept the request and add the new request's timestamp to the log -> return true.
	- if false, reject the request -> return false.
### Algorithm selection justification
From the 5 algorithms listed in the book, I chose the sliding window log algorithm for the following reasons:
1. The `RateLimiter` object from the problem statement in the *"MQR engineering take-home assignment"* email has the field `window_seconds`. From this I inferred that the suggested algorithm to choose for this task is a window-type algorithm.
2. I found it to be the most intuitive out of the window-type algorithms.
3. The algorithm logs every accepted request, so there is evidence of what it processes. This makes debugging easy and it makes it great as an educational tool.
4. It is accurate. It will properly enforce rate limiting within a specified window of time.

There are also cons to this approach:
1. It is memory inefficient. It saves up to `limit` number of timestamps in the log, so the space complexity is dependent on the `limit` of requests.
2. On average, it can be considered time inefficient when compared to other rate limiter algorithms. Iterating through the log to find and remove outdated timestamps on each call takes up more and more time as the `limit` of requests increases. The time complexity of the algorithm is linear and dependent on the `limit` of requests.
3. Thus, if the number of requests is very high, the algorithm might be too expensive to run and the tradeoff for accuracy not worth it.
### Difficulties faced in developing the Rate limiter
I have not yet covered data structures extensively or multithreaded programming at all in my personal education or at university as a first year CS student. The implementation of the rate limiter algorithm was definitely not an issue, but the multithreaded programming aspect required the use of generative AI.
### Additional comments
I would prefer to see a request object be passed to the `allow()` method instead of only the parameter `key` - a request object that contains the fields `key` and `timestamp`. I included the parameter `timestamp` in the signature of the `allow()` method as I found the initialization of the timestamp of a request to not be conceptually within the scope of a Rate Limiter, but I probably should've asked about this and not included a parameter that was not in the problem statement for this method.
