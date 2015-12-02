package com.felipecsl.elifut.adapter;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.felipecsl.elifut.R;
import com.felipecsl.elifut.activitiy.TeamDetailsActivity;
import com.felipecsl.elifut.models.Club;
import com.felipecsl.elifut.models.LeagueRound;
import com.felipecsl.elifut.models.Match;
import com.squareup.picasso.Picasso;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.util.List;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.ButterKnife;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LeagueNextMatchesAdapter
    extends RecyclerView.Adapter<LeagueNextMatchesAdapter.BaseViewHolder<Match>>
    implements StickyRecyclerHeadersAdapter<LeagueNextMatchesAdapter.BaseViewHolder<LeagueRound>> {
  private final List<Match> matches;
  private final Club currentClub;
  private final int totalRounds;
  private LeagueRound round;

  public LeagueNextMatchesAdapter(Club currentClub, int totalRounds, LeagueRound round) {
    this.currentClub = checkNotNull(currentClub);
    this.totalRounds = checkNotNull(totalRounds);
    this.round = checkNotNull(round);
    this.matches = checkNotNull(round).matches();
  }

  @Override public BaseViewHolder<Match> onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ViewHolder(parent, currentClub);
  }

  @Override public void onBindViewHolder(BaseViewHolder<Match> holder, int position) {
    holder.bind(matches.get(position));
  }

  @Override public long getHeaderId(int position) {
    return 0;
  }

  @Override public long getItemId(int position) {
    return matches.get(position).hashCode();
  }

  @Override public BaseViewHolder<LeagueRound> onCreateHeaderViewHolder(ViewGroup parent) {
    return new HeaderViewHolder(parent, totalRounds);
  }

  @Override public void onBindHeaderViewHolder(BaseViewHolder<LeagueRound> holder, int position) {
    holder.bind(round);
  }

  @Override public int getItemCount() {
    return matches.size();
  }

  public void setItems(LeagueRound round) {
    this.round = round;
    matches.clear();
    matches.addAll(round.matches());
    notifyDataSetChanged();
  }

  static abstract class BaseViewHolder<T> extends RecyclerView.ViewHolder
      implements BindableViewHolder<T> {
    BaseViewHolder(ViewGroup parent, @LayoutRes int layoutId) {
      super(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false));
    }
  }

  static class ViewHolder extends BaseViewHolder<Match> {
    private final Club currentClub;

    @Bind(R.id.outer_layout) FrameLayout layoutOuter;
    @Bind(R.id.layout_team_home) LinearLayout layoutTeamHome;
    @Bind(R.id.layout_team_away) LinearLayout layoutTeamAway;
    @Bind(R.id.img_team_home) ImageView imgTeamHome;
    @Bind(R.id.img_team_away) ImageView imgTeamAway;
    @Bind(R.id.txt_team_home) TextView txtTeamHome;
    @Bind(R.id.txt_team_away) TextView txtTeamAway;

    @BindColor(R.color.light_gray) int colorLightGray;
    @BindColor(android.R.color.transparent) int colorTransparent;

    ViewHolder(ViewGroup parent, Club currentClub) {
      super(parent, R.layout.adapter_future_match_item);
      this.currentClub = currentClub;
      ButterKnife.bind(this, itemView);
    }

    @Override public void bind(Match match) {
      Club home = match.home();
      Club away = match.away();

      Picasso.with(itemView.getContext())
          .load(home.large_image())
          .into(imgTeamHome);

      Picasso.with(itemView.getContext())
          .load(away.large_image())
          .into(imgTeamAway);

      txtTeamHome.setText(home.tinyName().toUpperCase());
      txtTeamAway.setText(away.tinyName().toUpperCase());
      Context context = itemView.getContext();
      layoutTeamAway.setOnClickListener(view -> context.startActivity(
          TeamDetailsActivity.newIntent(context, away)));
      layoutTeamHome.setOnClickListener(view -> context.startActivity(
          TeamDetailsActivity.newIntent(context, home)));

      layoutOuter.setBackgroundColor(
          getAdapterPosition() % 2 == 0 ? colorLightGray : colorTransparent);
    }
  }

  public static class HeaderViewHolder extends BaseViewHolder<LeagueRound> {
    private final int totalRounds;
    @Bind(R.id.txt_header) TextView txtHeader;

    HeaderViewHolder(ViewGroup parent, int totalRounds) {
      super(parent, R.layout.adapter_round_header);
      this.totalRounds = totalRounds;
      ButterKnife.bind(this, itemView);
    }

    @Override public void bind(LeagueRound round) {
      Context context = itemView.getContext();
      txtHeader.setText(context.getString(R.string.round_n_of_n, round.roundNumber(), totalRounds));
    }
  }
}