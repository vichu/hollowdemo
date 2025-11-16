package org.vish.hollowdemo.api;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.index.UniqueKeyIndex;
import com.netflix.hollow.api.objects.HollowObject;
import com.netflix.hollow.core.schema.HollowObjectSchema;

@SuppressWarnings("all")
public class Movie extends HollowObject {

    public Movie(MovieDelegate delegate, int ordinal) {
        super(delegate, ordinal);
    }

    public long getId() {
        return delegate().getId(ordinal);
    }

    public Long getIdBoxed() {
        return delegate().getIdBoxed(ordinal);
    }

    public HString getTitle() {
        int refOrdinal = delegate().getTitleOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getGenre() {
        int refOrdinal = delegate().getGenreOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public double getRating() {
        return delegate().getRating(ordinal);
    }

    public Double getRatingBoxed() {
        return delegate().getRatingBoxed(ordinal);
    }

    public int getReleaseYear() {
        return delegate().getReleaseYear(ordinal);
    }

    public Integer getReleaseYearBoxed() {
        return delegate().getReleaseYearBoxed(ordinal);
    }

    public int getDuration() {
        return delegate().getDuration(ordinal);
    }

    public Integer getDurationBoxed() {
        return delegate().getDurationBoxed(ordinal);
    }

    public HString getDescription() {
        int refOrdinal = delegate().getDescriptionOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getDirector() {
        int refOrdinal = delegate().getDirectorOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public ListOfString getCast() {
        int refOrdinal = delegate().getCastOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getListOfString(refOrdinal);
    }

    public ListOfString getWriters() {
        int refOrdinal = delegate().getWritersOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getListOfString(refOrdinal);
    }

    public HString getProductionCompany() {
        int refOrdinal = delegate().getProductionCompanyOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getCountry() {
        int refOrdinal = delegate().getCountryOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getLanguage() {
        int refOrdinal = delegate().getLanguageOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public long getBudget() {
        return delegate().getBudget(ordinal);
    }

    public Long getBudgetBoxed() {
        return delegate().getBudgetBoxed(ordinal);
    }

    public long getBoxOffice() {
        return delegate().getBoxOffice(ordinal);
    }

    public Long getBoxOfficeBoxed() {
        return delegate().getBoxOfficeBoxed(ordinal);
    }

    public ListOfString getTags() {
        int refOrdinal = delegate().getTagsOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getListOfString(refOrdinal);
    }

    public HString getAgeRating() {
        int refOrdinal = delegate().getAgeRatingOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public HString getAwards() {
        int refOrdinal = delegate().getAwardsOrdinal(ordinal);
        if(refOrdinal == -1)
            return null;
        return  api().getHString(refOrdinal);
    }

    public MovieAPI api() {
        return typeApi().getAPI();
    }

    public MovieTypeAPI typeApi() {
        return delegate().getTypeAPI();
    }

    protected MovieDelegate delegate() {
        return (MovieDelegate)delegate;
    }

    /**
     * Creates a unique key index for {@code Movie} that has a primary key.
     * The primary key is represented by the type {@code long}.
     * <p>
     * By default the unique key index will not track updates to the {@code consumer} and thus
     * any changes will not be reflected in matched results.  To track updates the index must be
     * {@link HollowConsumer#addRefreshListener(HollowConsumer.RefreshListener) registered}
     * with the {@code consumer}
     *
     * @param consumer the consumer
     * @return the unique key index
     */
    public static UniqueKeyIndex<Movie, Long> uniqueIndex(HollowConsumer consumer) {
        return UniqueKeyIndex.from(consumer, Movie.class)
            .bindToPrimaryKey()
            .usingPath("id", long.class);
    }

}