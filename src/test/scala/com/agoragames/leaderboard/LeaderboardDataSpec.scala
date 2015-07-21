package com.agoragames.leaderboard

import org.scalatest.Spec
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class LeaderboardDataSpec extends FunSpec
                        with BeforeAndAfterEach
                        with BeforeAndAfterAll {
                            
    describe("constructor") {
        it("should have the correct parameters") {
            var leaderboardData = new LeaderboardData("member", 5.0, 5)
            
            leaderboardData.member should equal("member")
            leaderboardData.score should equal(5.0)
            leaderboardData.rank should equal(5)
        }
    }
}