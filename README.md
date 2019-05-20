A simple project to reproduce a possible bug with calling retry on WebClient when using `jetty-reactive-httpclient`,
where a `CancellationException` is thrown whenever retry is called on WebClient.exchange.

The project has a simple RestController at `/api/repro` that hits a WebClient and returns a result.

The `ReproClient` class uses a WebClient to hit the postman-echo api and receives a 404 response. When it receives the 404 response
it throws a ResponseStatusException with BAD_GATEWAY, and then calls `retry(2)` to attempt the client call two more times.

You can switch back and forth between using the `jetty-reactive-httpclient` and the `reactor-netty` client by uncommenting the
the respective dependency in `pom.xml` and the WebClient bean in `WebClientConfiguration.java`.

*Expected Behavior (works with reactor-netty)*
--
Because the postman-echo endpoint always returns 404, we would expect a _successful_ test to call the echo endpoint three times in total,
and then return a ResponseStatusException with 502 BAD_GATEWAY back to the controller, which should respond with a message "Didn't get a 200, retrying...".

This is the behavior we see when using `reactor-netty` as the Http client connector. The logs for a successful execution look like this:

```
2019-05-20 13:19:58.469 DEBUG 11725 --- [ctor-http-nio-2] o.s.w.s.adapter.HttpWebHandlerAdapter    : [172b76f1] HTTP GET "/api/repro"
2019-05-20 13:19:58.491 DEBUG 11725 --- [ctor-http-nio-2] s.w.r.r.m.a.RequestMappingHandlerMapping : [172b76f1] Mapped to public reactor.core.publisher.Mono<org.springframework.http.ResponseEntity<?>> com.repro.ReproController.getRetryRequest()
2019-05-20 13:19:58.544 DEBUG 11725 --- [ctor-http-nio-2] o.s.w.r.f.client.ExchangeFunctions       : [2286107d] HTTP GET https://postman-echo.com/status/404
2019-05-20 13:19:59.252 DEBUG 11725 --- [ctor-http-nio-6] o.s.w.r.f.client.ExchangeFunctions       : [2286107d] Response 404 NOT_FOUND
2019-05-20 13:19:59.262 DEBUG 11725 --- [ctor-http-nio-6] o.s.w.r.f.client.ExchangeFunctions       : [2286107d] Cancel signal (to close connection)
2019-05-20 13:19:59.262 DEBUG 11725 --- [ctor-http-nio-6] o.s.w.r.f.client.ExchangeFunctions       : [2286107d] HTTP GET https://postman-echo.com/status/404
2019-05-20 13:19:59.478 DEBUG 11725 --- [ctor-http-nio-6] o.s.w.r.f.client.ExchangeFunctions       : [2286107d] Response 404 NOT_FOUND
2019-05-20 13:19:59.478 DEBUG 11725 --- [ctor-http-nio-6] o.s.w.r.f.client.ExchangeFunctions       : [2286107d] Cancel signal (to close connection)
2019-05-20 13:19:59.478 DEBUG 11725 --- [ctor-http-nio-6] o.s.w.r.f.client.ExchangeFunctions       : [2286107d] HTTP GET https://postman-echo.com/status/404
2019-05-20 13:19:59.551 DEBUG 11725 --- [ctor-http-nio-6] o.s.w.r.f.client.ExchangeFunctions       : [2286107d] Response 404 NOT_FOUND
2019-05-20 13:19:59.552 DEBUG 11725 --- [ctor-http-nio-6] o.s.w.r.f.client.ExchangeFunctions       : [2286107d] Cancel signal (to close connection)
2019-05-20 13:19:59.565 DEBUG 11725 --- [ctor-http-nio-6] a.w.r.e.AbstractErrorWebExceptionHandler : [172b76f1] Resolved [ResponseStatusException: 502 BAD_GATEWAY "Didn't get a 200, retrying..."] for HTTP GET /api/repro
2019-05-20 13:19:59.591 DEBUG 11725 --- [ctor-http-nio-6] o.s.http.codec.json.Jackson2JsonEncoder  : [172b76f1] Encoding [{timestamp=Mon May 20 13:19:59 PDT 2019, path=/api/repro, status=502, error=Bad Gateway, message=Did (truncated)...]
2019-05-20 13:19:59.636 DEBUG 11725 --- [ctor-http-nio-2] o.s.w.s.adapter.HttpWebHandlerAdapter    : [172b76f1] Completed 502 BAD_GATEWAY
```

*Actual Behavior (fails with jetty-reactive-httpclient)*
--
The first call to the postman-echo endpoint returns a 404. Then a `CancellationException` is thrown and returned from the publisher
back up to the Controller instead of successfully retrying the WebClient.exchange. The logs for the failing execution look like this:

```
2019-05-20 13:24:14.934 DEBUG 11744 --- [ctor-http-nio-2] o.s.w.s.adapter.HttpWebHandlerAdapter    : [29daf692] HTTP GET "/api/repro"
2019-05-20 13:24:14.955 DEBUG 11744 --- [ctor-http-nio-2] s.w.r.r.m.a.RequestMappingHandlerMapping : [29daf692] Mapped to public reactor.core.publisher.Mono<org.springframework.http.ResponseEntity<?>> com.repro.ReproController.getRetryRequest()
2019-05-20 13:24:15.191 DEBUG 11744 --- [ctor-http-nio-2] o.s.w.r.f.client.ExchangeFunctions       : [6feb0302] HTTP GET https://postman-echo.com/status/404
2019-05-20 13:24:15.687 DEBUG 11744 --- [ent@14229fa7-29] o.s.w.r.f.client.ExchangeFunctions       : [6feb0302] Response 404 NOT_FOUND
2019-05-20 13:24:15.692 DEBUG 11744 --- [ent@14229fa7-29] o.s.w.r.f.client.ExchangeFunctions       : [6feb0302] Cancel signal (to close connection)
2019-05-20 13:24:15.692 DEBUG 11744 --- [ent@14229fa7-29] o.s.w.r.f.client.ExchangeFunctions       : [6feb0302] HTTP GET https://postman-echo.com/status/404
2019-05-20 13:24:15.693 DEBUG 11744 --- [ent@14229fa7-29] o.s.w.r.f.client.ExchangeFunctions       : [6feb0302] HTTP GET https://postman-echo.com/status/404
2019-05-20 13:24:15.707 DEBUG 11744 --- [ent@14229fa7-29] a.w.r.e.AbstractErrorWebExceptionHandler : [29daf692] Resolved [CancellationException: null] for HTTP GET /api/repro
2019-05-20 13:24:15.714 ERROR 11744 --- [ent@14229fa7-29] a.w.r.e.AbstractErrorWebExceptionHandler : [29daf692] 500 Server Error for HTTP GET "/api/repro"

java.util.concurrent.CancellationException: null
	at org.eclipse.jetty.reactive.client.internal.AbstractSinglePublisher.subscribe(AbstractSinglePublisher.java:54) ~[jetty-reactive-httpclient-1.0.3.jar:na]
	at reactor.core.publisher.MonoFromPublisher.subscribe(MonoFromPublisher.java:43) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.drain(MonoIgnoreThen.java:153) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.MonoIgnoreThen.subscribe(MonoIgnoreThen.java:56) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.MonoPeekFuseable.subscribe(MonoPeekFuseable.java:74) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.MonoPeekFuseable.subscribe(MonoPeekFuseable.java:74) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.MonoMapFuseable.subscribe(MonoMapFuseable.java:59) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.MonoSwitchIfEmpty.subscribe(MonoSwitchIfEmpty.java:44) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.MonoFlatMap.subscribe(MonoFlatMap.java:60) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.Mono.subscribe(Mono.java:3710) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.FluxRetry$RetrySubscriber.resubscribe(FluxRetry.java:109) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.FluxRetry$RetrySubscriber.onError(FluxRetry.java:93) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:122) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.FluxSwitchIfEmpty$SwitchIfEmptySubscriber.onNext(FluxSwitchIfEmpty.java:67) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:121) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onNext(FluxPeekFuseable.java:204) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.FluxPeekFuseable$PeekFuseableSubscriber.onNext(FluxPeekFuseable.java:204) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1505) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.MonoIgnoreThen$ThenAcceptInner.onNext(MonoIgnoreThen.java:296) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.MonoNext$NextSubscriber.onNext(MonoNext.java:76) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at org.eclipse.jetty.reactive.client.internal.AbstractSingleProcessor.downStreamOnNext(AbstractSingleProcessor.java:110) ~[jetty-reactive-httpclient-1.0.3.jar:na]
	at org.eclipse.jetty.reactive.client.internal.ResponseListenerPublisher.onNext(ResponseListenerPublisher.java:130) ~[jetty-reactive-httpclient-1.0.3.jar:na]
	at reactor.core.publisher.StrictSubscriber.onNext(StrictSubscriber.java:89) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2070) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.StrictSubscriber.onSubscribe(StrictSubscriber.java:77) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.MonoJust.subscribe(MonoJust.java:54) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at reactor.core.publisher.Mono.subscribe(Mono.java:3710) ~[reactor-core-3.2.8.RELEASE.jar:3.2.8.RELEASE]
	at org.eclipse.jetty.reactive.client.internal.ResponseListenerPublisher.onHeaders(ResponseListenerPublisher.java:72) ~[jetty-reactive-httpclient-1.0.3.jar:na]
	at org.eclipse.jetty.client.ResponseNotifier.notifyHeaders(ResponseNotifier.java:98) ~[jetty-client-9.4.18.v20190429.jar:9.4.18.v20190429]
	at org.eclipse.jetty.client.ResponseNotifier.notifyHeaders(ResponseNotifier.java:90) ~[jetty-client-9.4.18.v20190429.jar:9.4.18.v20190429]
	at org.eclipse.jetty.client.HttpReceiver.responseHeaders(HttpReceiver.java:267) ~[jetty-client-9.4.18.v20190429.jar:9.4.18.v20190429]
	at org.eclipse.jetty.client.http.HttpReceiverOverHTTP.headerComplete(HttpReceiverOverHTTP.java:256) ~[jetty-client-9.4.18.v20190429.jar:9.4.18.v20190429]
	at org.eclipse.jetty.http.HttpParser.parseFields(HttpParser.java:1218) ~[jetty-http-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.http.HttpParser.parseNext(HttpParser.java:1502) ~[jetty-http-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.client.http.HttpReceiverOverHTTP.parse(HttpReceiverOverHTTP.java:172) ~[jetty-client-9.4.18.v20190429.jar:9.4.18.v20190429]
	at org.eclipse.jetty.client.http.HttpReceiverOverHTTP.process(HttpReceiverOverHTTP.java:135) ~[jetty-client-9.4.18.v20190429.jar:9.4.18.v20190429]
	at org.eclipse.jetty.client.http.HttpReceiverOverHTTP.receive(HttpReceiverOverHTTP.java:73) ~[jetty-client-9.4.18.v20190429.jar:9.4.18.v20190429]
	at org.eclipse.jetty.client.http.HttpChannelOverHTTP.receive(HttpChannelOverHTTP.java:133) ~[jetty-client-9.4.18.v20190429.jar:9.4.18.v20190429]
	at org.eclipse.jetty.client.http.HttpConnectionOverHTTP.onFillable(HttpConnectionOverHTTP.java:155) ~[jetty-client-9.4.18.v20190429.jar:9.4.18.v20190429]
	at org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:305) ~[jetty-io-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:103) ~[jetty-io-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.io.ssl.SslConnection$DecryptedEndPoint.onFillable(SslConnection.java:427) ~[jetty-io-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.io.ssl.SslConnection.onFillable(SslConnection.java:321) ~[jetty-io-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.io.ssl.SslConnection$2.succeeded(SslConnection.java:159) ~[jetty-io-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:103) ~[jetty-io-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.io.ChannelEndPoint$2.run(ChannelEndPoint.java:117) ~[jetty-io-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.runTask(EatWhatYouKill.java:333) ~[jetty-util-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.doProduce(EatWhatYouKill.java:310) ~[jetty-util-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.tryProduce(EatWhatYouKill.java:168) ~[jetty-util-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.run(EatWhatYouKill.java:126) ~[jetty-util-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.util.thread.ReservedThreadExecutor$ReservedThread.run(ReservedThreadExecutor.java:366) ~[jetty-util-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:765) ~[jetty-util-9.4.15.v20190215.jar:9.4.15.v20190215]
	at org.eclipse.jetty.util.thread.QueuedThreadPool$2.run(QueuedThreadPool.java:683) ~[jetty-util-9.4.15.v20190215.jar:9.4.15.v20190215]
	at java.base/java.lang.Thread.run(Thread.java:844) ~[na:na]

2019-05-20 13:24:15.734 DEBUG 11744 --- [ent@14229fa7-29] o.s.http.codec.json.Jackson2JsonEncoder  : [29daf692] Encoding [{timestamp=Mon May 20 13:24:15 PDT 2019, path=/api/repro, status=500, error=Internal Server Error, m (truncated)...]
2019-05-20 13:24:15.798 DEBUG 11744 --- [ctor-http-nio-2] o.s.w.s.adapter.HttpWebHandlerAdapter    : [29daf692] Completed 500 INTERNAL_SERVER_ERROR
```

