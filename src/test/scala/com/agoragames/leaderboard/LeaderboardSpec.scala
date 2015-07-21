package com.agoragames.leaderboard

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner
import org.scalatest.OptionValues._
import org.junit.runner.RunWith

import com.redis._

@RunWith(classOf[JUnitRunner])
class LeaderboardSpec extends FunSpec
                        with BeforeAndAfterEach
                        with BeforeAndAfterAll {
    val host = "localhost"
    val testLeaderboardName = "leaderboard_name"
    val redisClient = new RedisClient(host, 6379)
    var leaderboard = new Leaderboard(testLeaderboardName, host, 6379, 25)

    override def beforeEach = {
        redisClient.flushdb
    }

    override def afterEach = {
    }

    override def afterAll = {
        redisClient.disconnect
        leaderboard.disconnect
    }
    
    private def rankMembersInLeaderboard(totalMembers: Int) = {
        for (i <- 1 to totalMembers) {
            leaderboard.rankMember("member_" + i, i)
        }
    }
    
    describe("version") {
      it("should be the correct version") {
          leaderboard.version should equal("2.0.1")
      }
    }
    
    describe("constructor") {
        it("should have the correct parameters") {
            leaderboard.leaderboardName should equal(testLeaderboardName)
            leaderboard.pageSize should equal(25)
        }
        
        it("should be able to use an existing RedisClient instance") {
            leaderboard = new Leaderboard(testLeaderboardName, host, 6379, 25, scala.collection.mutable.HashMap[String, Object]("redis_connection" -> redisClient))
            rankMembersInLeaderboard(5)
            leaderboard.totalMembers.get should equal(5)
        }
    }
    
    describe("deleteLeaderboard and deleteLeaderboardNames") {
        it("should be able to delete the named leaderboard") {
            rankMembersInLeaderboard(5)
            
            redisClient.exists(testLeaderboardName) should equal(true)
            leaderboard.deleteLeaderboard
            redisClient.exists(testLeaderboardName) should equal(false)
        }
    }
        
    describe("totalMembers and totalMembersIn") {
        it("should return the correct number of members for totalMembers") {
            leaderboard.totalMembers should equal(Some(0))
            leaderboard.totalMembers.get should equal(0)
        }
        
        it("should return the correct number of members for totalMembersIn") {
            leaderboard.totalMembersIn(testLeaderboardName) should equal(Some(0))
            leaderboard.totalMembersIn(testLeaderboardName).get should equal(0)
        }
    }
    
    describe("rankMember and rankMemberIn") {
        it("should be able to add a member to the leaderboard using rankMember") {        
            leaderboard.rankMember("member", 1337).get should equal (1.toLong)
            leaderboard.totalMembersIn(testLeaderboardName).get should equal (1.toLong)

            leaderboard.rankMember("member", 1338).get should equal (0.toLong)
            leaderboard.totalMembersIn(testLeaderboardName).get should equal (1.toLong)
        }

        it("should be able to add a member to the leaderboard using rankMemberIn") {
            leaderboard.rankMemberIn(testLeaderboardName, "member", 1337).get should equal (1.toLong)
            leaderboard.totalMembersIn(testLeaderboardName).get should equal(1)

            leaderboard.rankMemberIn(testLeaderboardName, "member", 1338).get should equal(0)
            leaderboard.totalMembersIn(testLeaderboardName).get should equal(1)

            leaderboard.rankMemberIn(testLeaderboardName, "another_member", 1339).get should equal(1)
            leaderboard.totalMembersIn(testLeaderboardName).get should equal(2)
         }
    }
    
    describe("removeMember and removeMemberFrom") {
        it("should remove a member if they were added to a leaderboard") {
            leaderboard.rankMember("member", 1337).get should equal(1)
            leaderboard.totalMembers.get should equal(1)
            
            leaderboard.removeMember("member").get should equal(1)
            leaderboard.totalMembers.get should equal(0)
        }
    }
    
    describe("totalPages and totalPagesIn") {
        it("should return the correct number of pages in the leaderboard using totalPages for a single page") {
            rankMembersInLeaderboard(5)
            
            leaderboard.totalPages should equal(1)
        }

        it("should return the correct number of pages in the leaderboard using totalPages") {
            rankMembersInLeaderboard(LeaderboardDefaults.DEFAULT_PAGE_SIZE + 2)
            
            leaderboard.totalPages should equal(2)
        }

        it("should return the correct number of pages in the leaderboard using totalPagesIn") {
            rankMembersInLeaderboard(LeaderboardDefaults.DEFAULT_PAGE_SIZE + 2)
            
            leaderboard.totalPagesIn(testLeaderboardName, LeaderboardDefaults.DEFAULT_PAGE_SIZE) should equal(2)
        }
    }
    
    // RedisClient does not currently support zcount.
    // describe("totalMembersInScoreRange and totalMembersInScoreRangeIn") {
    //     it("should return correct number of members for totalMembersInScoreRange") {
    //         rankMembersInLeaderboard(5)
    //         
    //         leaderboard.totalMembersInScoreRange(2, 4) should equal(3)
    //     }
    // }
    
    describe("scoreFor and scoreforIn") {
        it("should return the correct score for a member using scoreFor") {
            rankMembersInLeaderboard(5)
            
            leaderboard.scoreFor("member_3").get should equal(3.0)
        }

        it("should return the correct score for a member using scoreForIn") {
            rankMembersInLeaderboard(5)
            
            leaderboard.scoreForIn(testLeaderboardName, "member_3").get should equal(3.0)
        }
    }

    describe("changeScoreFor and changeScoreforIn") {
        it("should return the correct score for a member using changeScoreFor") {
            rankMembersInLeaderboard(5)
            
            leaderboard.changeScoreFor("member_3", 6).get should equal(9.0)
            leaderboard.changeScoreFor("member_3", -3).get should equal(6.0)
        }

        it("should return the correct score for a member using changeScoreForIn") {
            rankMembersInLeaderboard(5)
            
            leaderboard.changeScoreForIn(testLeaderboardName, "member_3", 6).get should equal(9.0)
            leaderboard.changeScoreForIn(testLeaderboardName, "member_3", -3).get should equal(6.0)
        }
    }
    
    describe("checkMember and checkMemberIn") {
        it("should return whether or not a member is in the leaderboard using checkMember") {
            rankMembersInLeaderboard(5)

            leaderboard.totalMembers.get should equal(5)
            leaderboard.checkMember("member_3") should equal(true)
            leaderboard.checkMember("member_10") should equal(false)
        }

        it("should return whether or not a member is in the leaderboard using checkMemberIn") {
            rankMembersInLeaderboard(5)
            
            leaderboard.checkMemberIn(testLeaderboardName, "member_3") should equal(true)
            leaderboard.checkMemberIn(testLeaderboardName, "member_10") should equal(false)
        }
    }
    
    describe("rankFor and rankForIn") {
        it("should return the correct rank for rankFor") {
            rankMembersInLeaderboard(5)
            
            leaderboard.rankFor("member_4").get should equal(2)
            leaderboard.rankFor("member_4", true).get should equal(1)
        }
    }
    
    describe("scoreAndRankFor and scoreAndRankForIn") {
        it("should return the correct rank and score for scoreAndRankFor") {
            rankMembersInLeaderboard(5)
            
            var dataMap: scala.collection.mutable.HashMap[String, Object] = leaderboard.scoreAndRankFor("member_1")
            
            dataMap("member") should equal("member_1")
            dataMap("score").asInstanceOf[Option[Double]].get should equal(1.0)
            dataMap("rank").asInstanceOf[Option[Long]].get should equal(5.toLong)
        }
    }
    
    describe("removeMembersInScoreRange and removeMembersInScoreRangeIn") {
        it("should return the correct number of members removed for removeMembersInScoreRange") {
            rankMembersInLeaderboard(5)

            leaderboard.totalMembers.get should equal(5)

            leaderboard.rankMember("cheater_1", 100)
            leaderboard.rankMember("cheater_2", 101)
            leaderboard.rankMember("cheater_3", 102)

            leaderboard.totalMembers.get should equal(8)

            leaderboard.removeMembersInScoreRange(100, 102)

            leaderboard.totalMembers.get should equal(5)            
        }
    }
    
    describe("leaders and leadersIn") {
        it("should return the correct leaders in the leaderboard when calling leaders") {
            rankMembersInLeaderboard(25)
            
            leaderboard.totalMembers.get should equal(25)
            
            val leaders = leaderboard.leaders(1)

            leaders.length should equal(25)
            leaders(0)._1 should equal("member_25")
            leaders(0)._2 should equal(25.0)
            leaders(0)._3 should equal(1)
        }
        
        it("should return the correct leaders with multiple pages") {
            rankMembersInLeaderboard(LeaderboardDefaults.DEFAULT_PAGE_SIZE * 3 + 1)
            
            leaderboard.totalMembers.get should equal(LeaderboardDefaults.DEFAULT_PAGE_SIZE * 3 + 1)
            
            var leaders = leaderboard.leaders(1)
            
            leaders.size should equal(LeaderboardDefaults.DEFAULT_PAGE_SIZE)
            
            leaders = leaderboard.leaders(2)
            leaders.size should equal(LeaderboardDefaults.DEFAULT_PAGE_SIZE)

            leaders = leaderboard.leaders(3)
            leaders.size should equal(LeaderboardDefaults.DEFAULT_PAGE_SIZE)

            leaders = leaderboard.leaders(4)
            leaders.size should equal(1)

            leaders = leaderboard.leaders(-5)
            leaders.size should equal(LeaderboardDefaults.DEFAULT_PAGE_SIZE)

            leaders = leaderboard.leaders(10)
            leaders.size should equal(1)
        }
    }
    
    describe("aroundMe and aroundMeIn") {
        it("should return the correct leaders around me when calling aroundMe") {
            rankMembersInLeaderboard(LeaderboardDefaults.DEFAULT_PAGE_SIZE * 3 + 1)
            
            leaderboard.totalMembers.get should equal(LeaderboardDefaults.DEFAULT_PAGE_SIZE * 3 + 1)
            
            var leadersAroundMe = leaderboard.aroundMe("member_30")
            (leadersAroundMe.size / 2) should equal(LeaderboardDefaults.DEFAULT_PAGE_SIZE / 2)

            leadersAroundMe = leaderboard.aroundMe("member_1")
            (leadersAroundMe.size) should equal((LeaderboardDefaults.DEFAULT_PAGE_SIZE / 2) + 1)

            leadersAroundMe = leaderboard.aroundMe("member_76")
            (leadersAroundMe.size / 2) should equal(LeaderboardDefaults.DEFAULT_PAGE_SIZE / 2)
        }
    }
    
    describe("rankedInList and rankedInListIn") {
        it("should return the correct rank and score information when calling rankedInList") {
            rankMembersInLeaderboard(LeaderboardDefaults.DEFAULT_PAGE_SIZE)
            
            leaderboard.totalMembers.get should equal(LeaderboardDefaults.DEFAULT_PAGE_SIZE)
            
            var members: Array[String] = new Array[String](3)
            members(0) = "member_1"
            members(1) = "member_5"
            members(2) = "member_10"
            var rankedMembers = leaderboard.rankedInList(members)
            
            rankedMembers.size should equal(3)
            
            rankedMembers(0)._1 should equal("member_1")
            rankedMembers(0)._2 should equal(1)
            rankedMembers(0)._3 should equal(25.0)

            rankedMembers(1)._1 should equal("member_5")
            rankedMembers(1)._2 should equal(5.0)
            rankedMembers(1)._3 should equal(21)

            rankedMembers(2)._1 should equal("member_10")
            rankedMembers(2)._2 should equal(10.0)
            rankedMembers(2)._3 should equal(16)
        }
    }
}