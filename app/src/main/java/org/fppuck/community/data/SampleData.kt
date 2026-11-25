package org.fppuck.community.data

val sampleGames = listOf(
    HockeyGame(
        status = GameStatus.LIVE,
        time = "3rd • 12:48",
        awayTeam = "Carolina Thunderbirds",
        awayScore = 3,
        homeTeam = "Roanoke Rail Yard Dawgs",
        homeScore = 2
    ),

    HockeyGame(
        status = GameStatus.FINAL,
        awayTeam = "Pensacola Ice Flyers",
        awayScore = 5,
        homeTeam = "Birmingham Bulls",
        homeScore = 1
    ),

    HockeyGame(
        status = GameStatus.FINAL_OT,
        awayTeam = "Huntsville Havoc",
        awayScore = 4,
        homeTeam = "Macon Mayhem",
        homeScore = 3
    ),

    HockeyGame(
        status = GameStatus.UPCOMING,
        time = "7:00 PM",
        awayTeam = "Spokane Braves",
        homeTeam = "Quad City Storm"
    )
)

val sampleStandings = listOf(
    TeamStanding(
        rank = 1,
        teamName = "Roanoke Rail Yard Dawgs",
        gamesPlayed = 6,
        wins = 5,
        losses = 1,
        overtimeLosses = 0,
        points = 10
    ),

    TeamStanding(
        rank = 2,
        teamName = "Pensacola Ice Flyers",
        gamesPlayed = 6,
        wins = 4,
        losses = 2,
        overtimeLosses = 0,
        points = 8
    ),

    TeamStanding(
        rank = 3,
        teamName = "Huntsville Havoc",
        gamesPlayed = 6,
        wins = 4,
        losses = 2,
        overtimeLosses = 0,
        points = 8
    ),

    TeamStanding(
        rank = 4,
        teamName = "Macon Mayhem",
        gamesPlayed = 6,
        wins = 3,
        losses = 3,
        overtimeLosses = 0,
        points = 6
    ),

    TeamStanding(
        rank = 5,
        teamName = "Fayetteville Marksmen",
        gamesPlayed = 6,
        wins = 2,
        losses = 3,
        overtimeLosses = 1,
        points = 5
    ),

    TeamStanding(
        rank = 6,
        teamName = "Birmingham Bulls",
        gamesPlayed = 6,
        wins = 2,
        losses = 4,
        overtimeLosses = 0,
        points = 4
    )
)

val sampleTeams = listOf(

    Team(
        name = "Black Bears",
        city = "Binghamton",
        abbreviation = "BBB",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-e0049a17-cb58-4e26-9af9-dac4169bcada/1785216900-full.png"
    ),

    Team(
        name = "Hat Tricks",
        city = "Danbury",
        abbreviation = "DHT",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-1b3ed0a8-2703-481e-aed6-81e032bdd51b/1785216925-full.png"
    ),

    Team(
        name = "Bobcats",
        city = "Blue Ridge",
        abbreviation = "BRB",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-84de5b95-1854-4c0f-89f5-557b7758d38d/1785217009-full.png"
    ),

    Team(
        name = "Thunderbirds",
        city = "Twin City",
        abbreviation = "TCT",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-9a48a2dc-2124-443f-a421-ed506b47d408/1785217033-full.png"
    ),

    Team(
        name = "Sentinels",
        city = "Indiana",
        abbreviation = "IND",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-ad153e45-3e19-40d8-84bd-e6efece3f83d/1785217715-full.png"
    ),

    Team(
        name = "Rockers",
        city = "Motor City",
        abbreviation = "MCR",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-2bcd7cf2-24bb-4a5b-b754-2acafd6c8bf6/1785217764-full.jpg"
    ),

    Team(
        name = "Prowlers",
        city = "Port Huron",
        abbreviation = "PHP",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-9e26e286-d989-42d3-b61f-ec1982aba1b4/1785217794-full.png"
    ),

    Team(
        name = "Wolves",
        city = "Watertown",
        abbreviation = "WTW",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-cdb005ed-5593-404c-82d1-678cc9fa8107/1785217818-full.png"
    ),

    Team(
        name = "Monarchs",
        city = "Mid-South",
        abbreviation = "MSM",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-9ded91a3-6ae3-40dd-8e17-c566789460a1/1785218416-full.png"
    ),

    Team(
        name = "Moccasins",
        city = "Monroe",
        abbreviation = "MRM",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-338dc68d-148c-4a4f-9518-733a647b5c7f/1785218707-full.png"
    ),

    Team(
        name = "Kingfish",
        city = "Baton Rouge",
        abbreviation = "BKF",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-e04882dd-5e3a-4337-b255-91e8061d7dbc/1785218732-full.jpg"
    ),

    Team(
        name = "River Dragons",
        city = "Columbus",
        abbreviation = "CRD",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-f2188d86-9b4c-4d4c-811b-1a35d2a50d7d/1785900045-full.png"
    ),

    Team(
        name = "Northern Lights",
        city = "Minnesota",
        abbreviation = "MNL",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-fe583096-3d6c-46d3-8c14-f72b487123ab/1785218940-full.png"
    ),

    Team(
        name = "Scarecrows",
        city = "Topeka",
        abbreviation = "TOP",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-61079fec-df0e-4b95-9f1f-6337b23fc32c/1785218530-full.png"
    ),

    Team(
        name = "Thunder",
        city = "Stockton",
        abbreviation = "SCK",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-e906e309-2ac8-41bc-965e-599b2fab9acc/1785218986-full.png"
    ),

    Team(
        name = "Falcons",
        city = "Fresno",
        abbreviation = "FRF",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-47675b48-64d0-47ea-9e48-314278939632/1785219006-full.png"
    ),

    Team(
        name = "Shock",
        city = "Oceanside",
        abbreviation = "OCS",
        logoUrl = "https://digitalshift-assets.sfo2.cdn.digitaloceanspaces.com/pw/603c74d0-0ed2-4d3f-8171-b447f9398da4/p-4b96ae43-9df3-4ae2-8508-3436cefe8c17/1785899879-full.png"
    )
)