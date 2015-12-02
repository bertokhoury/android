package com.felipecsl.elifut.match;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.felipecsl.elifut.BuildConfig;
import com.felipecsl.elifut.models.Club;
import com.felipecsl.elifut.models.ClubStatistics;
import com.felipecsl.elifut.models.Goal;
import com.felipecsl.elifut.models.Goals;
import com.felipecsl.elifut.models.Match;
import com.felipecsl.elifut.models.MatchEvent;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.schedulers.Schedulers;

public class DefaultMatchStatistics implements MatchStatistics, Parcelable {
  private static final String TAG = DefaultMatchStatistics.class.getSimpleName();
  private final Club home;
  private final Club away;

  private final Club winner;
  private final List<Goal> winnerGoals;
  private final List<Goal> loserGoals;

  private DefaultMatchStatistics(Parcel source) {
    ClassLoader classLoader = getClass().getClassLoader();
    home = source.readParcelable(classLoader);
    away = source.readParcelable(classLoader);
    winner = source.readParcelable(classLoader);
    winnerGoals = (List<Goal>) source.createTypedArrayList(Goal.creator());
    loserGoals = (List<Goal>) source.createTypedArrayList(Goal.creator());
  }

  public DefaultMatchStatistics(Match match) {
    this(match, new Well19937c(), MatchStatistics.GOALS_DISTRIBUTION);
  }

  public DefaultMatchStatistics(Match match, RandomGenerator random,
      RealDistribution goalsDistribution) {
    this.home = match.home();
    this.away = match.away();

    float result = random.nextFloat();
    Log.d(TAG, "winner result was " + result);
    if (result <= MatchStatistics.HOME_WIN_PROBABILITY) {
      winner = home;
    } else if (result <= MatchStatistics.DRAW_PROBABILITY) {
      winner = null;
    } else {
      winner = away;
    }
    int totalGoals = Math.max((int) Math.round(goalsDistribution.sample()), 0);
    if (!isDraw()) {
      if (totalGoals <= 2) {
        winnerGoals = Goals.create(random, Math.max(totalGoals, 1), winner());
        loserGoals = Collections.emptyList();
      } else {
        // 3+ goals
        loserGoals = Goals.create(random, random.nextInt(Math.max(1, (totalGoals / 2) - 1)), loser());
        winnerGoals = Goals.create(random, totalGoals - loserGoals.size(), winner());
      }
    } else {
      int evenGoals = (totalGoals % 2 == 0) ? totalGoals : totalGoals + 1;
      winnerGoals = Goals.create(random, evenGoals / 2, home);
      loserGoals = Goals.create(random, evenGoals / 2, away);
    }
  }

  @Override public Club home() {
    return home;
  }

  @Override public Club away() {
    return away;
  }

  @Override public Club winner() {
    return winner;
  }

  @Override @Nullable public Club loser() {
    if (isDraw()) {
      return null;
    }
    return isHomeWin() ? away : home;
  }

  @Override public boolean isHomeWin() {
    return !isDraw() && winner.equals(home);

  }

  @Override public boolean isAwayWin() {
    return !isDraw() && winner.equals(away);

  }

  @Override public boolean isDraw() {
    return winner == null;
  }

  @Override public String finalScore() {
    return homeGoals().size() + "x" + awayGoals().size();
  }

  @Override public List<Goal> homeGoals() {
    return isHomeWin() ? winnerGoals : loserGoals;
  }

  @Override public List<Goal> awayGoals() {
    return isAwayWin() ? winnerGoals : loserGoals;
  }

  @Override public String refereeName() {
    return "John Doe";
  }

  @Override public ClubStatistics homeStatistics() {
    return ClubStatistics.create(0, 0, 0, 0, 0, 0, 0, 0);
  }

  @Override public ClubStatistics awayStatistics() {
    return ClubStatistics.create(0, 0, 0, 0, 0, 0, 0, 0);
  }

  @Override public int describeContents() {
    return 0;
  }

  @Override public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(home, 0);
    dest.writeParcelable(away, 0);
    dest.writeParcelable(winner, 0);
    dest.writeTypedList(winnerGoals);
    dest.writeTypedList(loserGoals);
  }

  public static Creator<DefaultMatchStatistics> CREATOR = new Creator<DefaultMatchStatistics>() {
    @Override public DefaultMatchStatistics createFromParcel(Parcel source) {
      return new DefaultMatchStatistics(source);
    }

    @Override public DefaultMatchStatistics[] newArray(int size) {
      return new DefaultMatchStatistics[size];
    }
  };

  public Observable<MatchEvent> eventsObservable(final int elapsedTime) {
    Observable<Goal> winnersGoalsObservable = Observable.from(winnerGoals);
    Observable<Goal> loserGoalsObservable = Observable.from(loserGoals);

    return Observable.merge(winnersGoalsObservable, loserGoalsObservable)
        .observeOn(Schedulers.io())
        .flatMap((goal) -> {
          int delay = goal.time() - elapsedTime;
          if (BuildConfig.DEBUG) {
            delay /= 10;
          }
          return Observable.<MatchEvent>just(goal).delay(delay, TimeUnit.SECONDS);
        }).skip(elapsedTime, TimeUnit.SECONDS);
  }
}