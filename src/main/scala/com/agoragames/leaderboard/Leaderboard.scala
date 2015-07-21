package com.agoragames.leaderboard

import java.lang

import com.redis._

object LeaderboardDefaults {
    val VERSION = "2.0.1"
    val DEFAULT_PAGE_SIZE = 25
    val DEFAULT_REDIS_HOST = "leaderboard-001.uj3orx.0001.use1.cache.amazonaws.com"
    val DEFAULT_REDIS_PORT = 6379   
}

class Leaderboard(leaderboardNameParam: String, 
        host: String = LeaderboardDefaults.DEFAULT_REDIS_HOST, 
        port: Int = LeaderboardDefaults.DEFAULT_REDIS_PORT, 
        pageSizeParam: Int = LeaderboardDefaults.DEFAULT_PAGE_SIZE,
        redisOptions: scala.collection.mutable.HashMap[String, Object] = scala.collection.mutable.HashMap("host" -> LeaderboardDefaults.DEFAULT_REDIS_HOST, "port" -> LeaderboardDefaults.DEFAULT_REDIS_PORT.asInstanceOf[AnyRef])) {
    private var redisClient: RedisClient = _;
    
    if (redisOptions.get("redis_connection") != None) {
        redisClient = redisOptions.get("redis_connection").get.asInstanceOf[RedisClient]
    } else {
        redisClient = new RedisClient(host, port)
    }
    
    val version = LeaderboardDefaults.VERSION
    val leaderboardName: String = leaderboardNameParam
    
    var pageSize: Int = pageSizeParam
    
    if (pageSize < 1) {
        pageSize = LeaderboardDefaults.DEFAULT_PAGE_SIZE
    }
    
    def disconnect: Boolean = {
        redisClient.disconnect
    }
    
    def deleteLeaderboard: Option[Long] = {
        this.deleteLeaderboardNamed(this.leaderboardName)    
    }
    
    def deleteLeaderboardNamed(leaderboardName: String): Option[Long] = {
        redisClient.del(leaderboardName)
    }
        
    def totalMembers: Option[Long] = {
        this.totalMembersIn(this.leaderboardName)
    }
        
    def totalMembersIn(leaderboardName: String): Option[Long] = {
        redisClient.zcard(leaderboardName)
    }

    def rankMember(memberName: String, score: Double): Option[Long] = {
        this.rankMemberIn(this.leaderboardName, memberName, score)
    }
    
    def rankMemberIn(leaderboardName: String, memberName: String, score: Double): Option[Long] = {
        redisClient.zadd(leaderboardName, score, memberName)
    }
    
    def removeMember(memberName: String): Option[Long] = {
        this.removeMemberFrom(this.leaderboardName, memberName)
    }
    
    def removeMemberFrom(leaderboardName: String, memberName: String): Option[Long] = {
        redisClient.zrem(leaderboardName, memberName)
    }
    
    def totalPages: Int = {
        this.totalPagesIn(this.leaderboardName, this.pageSize)
    }
    
    def totalPagesIn(leaderboardName: String, pageSize: Int): Int = {
        scala.math.ceil(this.totalMembersIn(leaderboardName).get.asInstanceOf[Float] / pageSize.asInstanceOf[Float]).asInstanceOf[Int]
    }

    // RedisClient does not currently support zcount.
    // def totalMembersInScoreRange(minScore: Double, maxScore: Double): Int = {
    //     this.totalMembersInScoreRangeIn(this.leaderboardName, minScore, maxScore)
    // }
    // 
    // def totalMembersInScoreRangeIn(leaderboardName: String, minScore: Double, maxScore: Double): Int = {
    //     redisClient.zcount(leaderboardName, minScore, maxScore)
    // }
    
    def scoreFor(member: String): Option[Double] = {
        this.scoreForIn(this.leaderboardName, member)
    }
    
    def scoreForIn(leaderboardName: String, member: String): Option[Double] = {
        redisClient.zscore(leaderboardName, member)
    }
    
    def changeScoreFor(member: String, score: Double): Option[Double] = {
        this.changeScoreForIn(this.leaderboardName, member, score)
    }
    
    def changeScoreForIn(leaderboardName: String, member: String, score: Double): Option[Double] = {
        redisClient.zincrby(leaderboardName, score, member)
    }

    def checkMember(member: String): Boolean = {
        this.checkMemberIn(this.leaderboardName, member)
    }

    def checkMemberIn(leaderboardName: String, member: String): Boolean = {
        !(redisClient.zscore(leaderboardName, member) == None)
    }

    def rankFor(member: String, useZeroIndexForRank: Boolean = false): Option[Long] = {
        this.rankForIn(this.leaderboardName, member, useZeroIndexForRank)
    }
    
    def rankForIn(leaderboardName: String, member: String, useZeroIndexForRank: Boolean = false): Option[Long] = {
        if (useZeroIndexForRank) {
            redisClient.zrank(leaderboardName, member, true)            
        } else {
            // This feels "not elegant"
            Some(new lang.Long((redisClient.zrank(leaderboardName, member, true).get + 1)))
        }
    }
    
    def scoreAndRankFor(member: String, useZeroIndexForRank: Boolean = false): scala.collection.mutable.HashMap[String, Object] = {
        this.scoreAndRankForIn(this.leaderboardName, member, useZeroIndexForRank)
    }
    
    def scoreAndRankForIn(leaderboardName: String, member: String, useZeroIndexForRank: Boolean = false): scala.collection.mutable.HashMap[String, Object] = {
        var dataMap = scala.collection.mutable.HashMap.empty[String, Object]
        var responses: List[Any] = redisClient.pipeline { transaction =>
            transaction.zscore(leaderboardName, member)
            transaction.zrank(leaderboardName, member, true)
        }.get
                
        dataMap += ("member" -> member)
        dataMap += ("score" -> responses(0).asInstanceOf[Option[Double]])
        if (!useZeroIndexForRank) {
            dataMap += ("rank" -> Some(responses(1).asInstanceOf[Option[Long]].get + 1))
        } else {
            dataMap += ("rank" -> Some(responses(1).asInstanceOf[Option[Long]]))
        }     
                
        dataMap
    }

    def removeMembersInScoreRange(minScore: Double, maxScore: Double): Option[Long] = {
        this.removeMembersInScoreRangeIn(this.leaderboardName, minScore, maxScore)
    }
    
    def removeMembersInScoreRangeIn(leaderboardName: String, minScore: Double, maxScore: Double): Option[Long] = {
        redisClient.zremrangebyscore(leaderboardName, minScore, maxScore)
    }
    
    def leaders(currentPage: Int, withScores: Boolean = true, withRank: Boolean = true, useZeroIndexForRank: Boolean = false, pageSize: Int = LeaderboardDefaults.DEFAULT_PAGE_SIZE) = {
        this.leadersIn(this.leaderboardName, currentPage, withScores, withRank, useZeroIndexForRank, pageSize)
    }
    
    def leadersIn(leaderboardName: String, currentPageParam: Int, withScores: Boolean = true, withRank: Boolean = true, useZeroIndexForRank: Boolean = false, pageSize: Int = LeaderboardDefaults.DEFAULT_PAGE_SIZE): List[(String, Double, Long)] = {
        var currentPage: Int = currentPageParam
        
        if (currentPage < 1) {
            currentPage = 1
        }
        
        if (currentPage > totalPagesIn(leaderboardName, pageSize)) {
            currentPage = totalPagesIn(leaderboardName, pageSize)
        }
        
        var indexForRedis: Int = currentPage - 1
        var startingOffset: Int = (indexForRedis * pageSize)
        if (startingOffset < 0) {
            startingOffset = 0
        }
        var endingOffset: Int = (startingOffset + pageSize) - 1
                
        var rawLeaderData = redisClient.zrange(leaderboardName, startingOffset, endingOffset, RedisClient.DESC)
        var massagedLeaderData = List[(String, Double, Long)]()

        rawLeaderData match{
            case Some(data) =>
            case _ =>
        }

        if (rawLeaderData != None) {
            var responses = redisClient.pipeline { transaction =>
                for (leader <- rawLeaderData.get) {
                    transaction.zscore(leaderboardName, leader)
                    transaction.zrank(leaderboardName, leader, true)
                }
            }.get
        
            for (leaderIndex <- rawLeaderData.get.indices) {
                var rank = responses(leaderIndex * 2 + 1).asInstanceOf[Some[Long]].get
            
                if (!useZeroIndexForRank) {
                    rank += 1
                }

                massagedLeaderData = massagedLeaderData ::: List[(String, Double, Long)]((rawLeaderData.get(leaderIndex), responses(leaderIndex * 2).asInstanceOf[Some[Double]].get, rank))
            }
        }
        
        massagedLeaderData        
    }
    
    def aroundMe(member: String, withScores: Boolean = true, withRank: Boolean = true, useZeroIndexForRank: Boolean = false, pageSize: Int = LeaderboardDefaults.DEFAULT_PAGE_SIZE): List[(String, Double, Long)] = {
        this.aroundMeIn(this.leaderboardName, member, withScores, withRank, useZeroIndexForRank, pageSize)      
    }
    
    def aroundMeIn(leaderboardName: String, member: String, withScores: Boolean = true, withRank: Boolean = true, useZeroIndexForRank: Boolean = false, pageSize: Int = LeaderboardDefaults.DEFAULT_PAGE_SIZE): List[(String, Double, Long)] = {
        var reverseRankForMember: Long = redisClient.zrank(leaderboardName, member, true).get

        var startingOffset: Int = (reverseRankForMember - (pageSize / 2)).toInt
        if (startingOffset < 0) {
          startingOffset = 0
        }
        var endingOffset = (startingOffset + pageSize) - 1

        var rawLeaderData = redisClient.zrange(leaderboardName, startingOffset, endingOffset, RedisClient.DESC)
        var massagedLeaderData: List[(String, Double, Long)] = List[(String, Double, Long)]()

        if (rawLeaderData != None) {
            var responses = redisClient.pipeline { transaction =>
                for (leader <- rawLeaderData.get) {
                    transaction.zscore(leaderboardName, leader)
                    transaction.zrank(leaderboardName, leader, true)
                }
            }.get
        
            for (leaderIndex <- rawLeaderData.get.indices) {
                var rank = responses(leaderIndex * 2 + 1).asInstanceOf[Some[Long]].get
            
                if (!useZeroIndexForRank) {
                    rank += 1
                }

              massagedLeaderData = massagedLeaderData :::  List[(String, Double, Long)]((rawLeaderData.get(leaderIndex), responses(leaderIndex * 2).asInstanceOf[Some[Double]].get, rank))
            }
        }
        massagedLeaderData
    }
    
    def rankedInList(members: Array[String], withScores: Boolean = true, useZeroIndexForRank: Boolean = false): List[(String, Double, Long)] = {
        rankedInListIn(this.leaderboardName, members, withScores, useZeroIndexForRank)
    }
    
    def rankedInListIn(leaderboardName: String, members: Array[String], withScores: Boolean = true, useZeroIndexForRank: Boolean = false): List[(String, Double, Long)] = {
        var ranksForMembers: List[(String, Double, Long)] = List[(String, Double, Long)]()
        
        var responses = redisClient.pipeline { transaction =>
            for (member <- members) {
                transaction.zscore(leaderboardName, member)
                transaction.zrank(leaderboardName, member, true)
            }
        }.get
    
        for (memberIndex <- members.indices) {
            var rank = responses(memberIndex * 2 + 1).asInstanceOf[Some[Long]].get
        
            if (!useZeroIndexForRank) {
                rank += 1
            }

          ranksForMembers = ranksForMembers ::: List[(String, Double, Long)]((members(memberIndex), responses(memberIndex * 2).asInstanceOf[Some[Double]].get, rank))
        }
        ranksForMembers
    }
}