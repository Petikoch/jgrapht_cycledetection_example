package ch.petikoch.examples.jgrapht

import org.jgrapht.DirectedGraph
import org.jgrapht.alg.CycleDetector
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleDirectedGraph
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SuppressWarnings("GroovyPointlessBoolean")
class JGraphT_CycleDetection_Example extends Specification {

	def 'cycle detection: simple triangle cycle is detected'() {
		given:
		DirectedGraph<String, DefaultEdge> directedGraph = new SimpleDirectedGraph<>(DefaultEdge.class)

		directedGraph.addVertex("t1");
		directedGraph.addVertex("t2");
		directedGraph.addVertex("t3");

		directedGraph.addEdge("t1", "t2");
		directedGraph.addEdge("t2", "t3");
		directedGraph.addEdge("t3", "t1");

		and:
		def cycleDetector = new CycleDetector(directedGraph)

		when:
		def hasCycles = cycleDetector.detectCycles()
		then:
		hasCycles == true

		when:
		def t1Involved = cycleDetector.detectCyclesContainingVertex("t1")
		then:
		t1Involved == true

		when:
		def t2Involved = cycleDetector.detectCyclesContainingVertex("t2")
		then:
		t2Involved == true

		when:
		def t3Involved = cycleDetector.detectCyclesContainingVertex("t3")
		then:
		t3Involved == true

		when:
		def tasksInCycle = cycleDetector.findCycles()
		then:
		tasksInCycle == ["t1", "t2", "t3"] as Set
	}

	def 'cycle detection: tasks outside of the cycle but dependent on a task of the cycle are not reported'() {
		given:
		DirectedGraph<String, DefaultEdge> directedGraph = new SimpleDirectedGraph<>(DefaultEdge.class)

		directedGraph.addVertex("cycleTask1");
		directedGraph.addVertex("cycleTask2");

		directedGraph.addEdge("cycleTask1", "cycleTask2");
		directedGraph.addEdge("cycleTask2", "cycleTask1");

		directedGraph.addVertex("outsideCycleTask1");
		directedGraph.addVertex("outsideCycleTask2");

		directedGraph.addEdge("outsideCycleTask1", "cycleTask2");
		directedGraph.addEdge("outsideCycleTask2", "outsideCycleTask1");

		and:
		def cycleDetector = new CycleDetector(directedGraph)

		when:
		def hasCycles = cycleDetector.detectCycles()
		then:
		hasCycles == true

		when:
		def cycleTask1Involved = cycleDetector.detectCyclesContainingVertex("cycleTask1")
		then:
		cycleTask1Involved == true

		when:
		def cycleTask2Involved = cycleDetector.detectCyclesContainingVertex("cycleTask2")
		then:
		cycleTask2Involved == true

		when:
		def outsideCycleTask1 = cycleDetector.detectCyclesContainingVertex("outsideCycleTask1")
		then:
		outsideCycleTask1 == false // true -> not detected, dependent ON a task of the cycle

		when:
		def outsideCycleTask2 = cycleDetector.detectCyclesContainingVertex("outsideCycleTask1")
		then:
		outsideCycleTask2 == false // true -> not detected, indirectly dependent ON a task of the cycle

		when:
		def tasksInCycle = cycleDetector.findCycles()
		then:
		tasksInCycle == ["cycleTask1", "cycleTask2"] as Set
	}

	def 'cycle detection: two distinct cycles are not reported as distinct cycles'() {
		given:
		DirectedGraph<String, DefaultEdge> directedGraph = new SimpleDirectedGraph<>(DefaultEdge.class)

		directedGraph.addVertex("cycleTask1");
		directedGraph.addVertex("cycleTask2");

		directedGraph.addEdge("cycleTask1", "cycleTask2");
		directedGraph.addEdge("cycleTask2", "cycleTask1");

		directedGraph.addVertex("cycleTask11");
		directedGraph.addVertex("cycleTask12");

		directedGraph.addEdge("cycleTask11", "cycleTask12");
		directedGraph.addEdge("cycleTask12", "cycleTask11");

		and:
		def cycleDetector = new CycleDetector(directedGraph)

		when:
		def hasCycles = cycleDetector.detectCycles()
		then:
		hasCycles == true

		when:
		def tasksInCycle = cycleDetector.findCycles()
		then:
		tasksInCycle == ["cycleTask1", "cycleTask2", "cycleTask11", "cycleTask12"] as Set
	}

	def 'cycle detection: SimpleDirectedGraph is not thread-safe'() {
		given:
		DirectedGraph<String, DefaultEdge> directedGraph = new SimpleDirectedGraph<>(DefaultEdge.class)

		and:
		def numberOfThreads = Runtime.getRuntime().availableProcessors() * 8
		def numberOfVertexesPerThread = 100

		and:
		def expectedVertexesInGraph = [] as Set
		numberOfThreads.times { int threadNo ->
			numberOfVertexesPerThread.times { int vertexNo ->
				def vertexName = "Vertex ${vertexNo} of thread ${threadNo}"
				expectedVertexesInGraph.add(vertexName)
			}
		}

		and:
		def threadsCanStartCountdownLatch = new CountDownLatch(numberOfThreads)
		def threadsAreDoneCountdownLatch = new CountDownLatch(numberOfThreads)

		when:
		numberOfThreads.times { int threadNo ->
			Thread.startDaemon("${JGraphT_CycleDetection_Example.class.simpleName}-thread-${threadNo}") {
				threadsCanStartCountdownLatch.countDown()
				threadsCanStartCountdownLatch.await(1, TimeUnit.MINUTES)

				numberOfVertexesPerThread.times { int vertexNo ->
					def vertexName = "Vertex ${vertexNo} of thread ${threadNo}"
					directedGraph.addVertex(vertexName);
				}

				threadsAreDoneCountdownLatch.countDown()
			}
		}
		threadsAreDoneCountdownLatch.await(1, TimeUnit.MINUTES)

		then:
		// doesn't work, because not thread-safe: directedGraph.vertexSet().size() == numberOfThreads * numberOfVertexesPerThread
		// doesn't work, because not thread-safe: directedGraph.vertexSet().containsAll(expectedVertexesInGraph)
		expectedVertexesInGraph.containsAll(directedGraph.vertexSet())
	}
}
