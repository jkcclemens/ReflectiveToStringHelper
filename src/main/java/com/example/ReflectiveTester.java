package com.example;

import com.example.ReflectiveToStringHelper.Include;

public final class ReflectiveTester {

    public static class Person {

        public String firstName = "Joe";
        public String lastName = "Schmoe";
        public final float height = 6.083F; // Joe is done growing. He's 6'1".
        public float age = 23.4F;
        protected int numberOfFriends = 3;
        protected int numberOfExes = 2;
        private boolean inARelationship = false;
        private Person partner = null;
        private boolean thinksHeIsGreat = true;
        private volatile int timesCried = 100;

        // As you'll see, we only work with fields. This never shows up anywhere.
        public String getLastSaidSentence() {
            return "Howdy!";
        }

        @Override
        public String toString() {
            return ReflectiveToStringHelper.of(this);
        }
    }

    private static int i = 0;

    private static void log(final Object o) {
        System.out.println(String.valueOf(++i) + ". " + (o == null ? "null" : o.toString()));
    }

    public static void main(final String[] args) {
        final Person joe = new Person();
        // 1. Let's only print what someone in the public would know about Joe.
        // 1. Person{firstName=Joe,lastName=Schmoe,age=23.4,height=6.083}
        log(
            ReflectiveToStringHelper.of(
                joe
            )
        );
        // 2. How about his friends?
        // 2. Person{height=6.083,firstName=Joe,lastName=Schmoe,age=23.4,numberOfFriends=3,numberOfExes=2}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().publics(true).protecteds(true)
            )
        );
        // 3. The things only Joe knows
        // 3. Person{inARelationship=false,partner=null,thinksHeIsGreat=true,timesCried=100}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().privates(true)
            )
        );
        // 4. What someone in the public knows and ONLY his amount of friends
        // 4. Person{firstName=Joe,lastName=Schmoe,age=23.4,height=6.083,numberOfFriends=3}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().publics(true).ensure("numberOfFriends")
            )
        );
        // 5. Joe wants people to think he's in a relationship
        // 5. Person{firstName=Joe,lastName=Schmoe,age=23.4,height=6.083,inARelationship=true}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().publics(true).map("thinksHeIsGreat", "inARelationship").ensure("thinksHeIsGreat")
            )
        );
        // 6. Joe is a really open guy, except about his exes.
        // 6. Person{height=6.083,firstName=Joe,lastName=Schmoe,age=23.4,numberOfFriends=3,inARelationship=false,partner=null,thinksHeIsGreat=true,timesCried=100}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().allVisibilities(true).exclude("numberOfExes")
            )
        );
        // 7. Joe isn't really a numbers guy with his family.
        // 7. Person{firstName=Joe,lastName=Schmoe}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().publics(true).protecteds(true).exclude(Integer.TYPE).exclude(Float.TYPE)
            )
        );
        // 8. Joe wants everyone to know the private on/off parts of his life, but nothing else.
        // 8. Person{inARelationship=false,thinksHeIsGreat=true}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().ensure(Boolean.TYPE)
            )
        );
        // 9. The only number Joe really likes is his height.
        // 9. Person{height=6.083}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().ensure("height", Float.TYPE)
            )
        );
        // 10. Joe doesn't want people to know the fixed parts of his life.
        // 10. Person{firstName=Joe,lastName=Schmoe,age=23.4}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().publics(true).keepFinals(false)
            )
        );
        // 11. Joe just can't control what makes him cry. He doesn't want people to know.
        // 11. Person{inARelationship=false,partner=null,thinksHeIsGreat=true}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().privates(true).keepVolatiles(false)
            )
        );
        // 12. Joe doesn't want to appear arrogant or weak!
        // 12. Person{inARelationship=false,partner=null}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().privates(true).exclude("thinksHeIsGreat").exclude("timesCried")
            )
        );
        // 13. Joe wants to share all of the positive aspects of his life.
        // 13. Person{thinksHeIsGreat=true}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().ensureValue(true)
            )
        );
        // 14. When Joe is single, he's proud of it. He will only share his relationship status if he's single.
        // 14. Person{inARelationship=false}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().ensure("inARelationship", Boolean.TYPE, false)
            )
        );
        // 15. Joe likes to alphabetize his attributes.
        // Person{age=23.4,firstName=Joe,height=6.083,inARelationship=false,lastName=Schmoe,numberOfExes=2,numberOfFriends=3,partner=null,thinksHeIsGreat=true,timesCried=100}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().allVisibilities(true),
                (f1, f2) -> f1.getName().compareTo(f2.getName())
            )
        );
        // 16. Joe thinks the empty parts of his life don't need sharing.
        // 16. Person{height=6.083,firstName=Joe,lastName=Schmoe,age=23.4,numberOfFriends=3,numberOfExes=2,inARelationship=false,thinksHeIsGreat=true,timesCried=100}
        log(
            ReflectiveToStringHelper.of(
                joe,
                Include.create().allVisibilities(true).omitNullValues(true)
            )
        );
    }

}
