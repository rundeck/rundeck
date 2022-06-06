import Vue from 'vue'
import { addons } from '@storybook/addons'


export default {
    title: 'Layouts/Sidebars'
}

export const Sidebar = () => (Vue.extend({
    render(h) {
        return (
            <section class="layout-sidebar">
                <aside>
                    <div class="layout-sidebar--sidebar-header">
                        <h1>System Configuration</h1>
                    </div>
                    <div class="layout-sidebar--sidebar-body">
                        <ul>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                            <li>sdf</li>
                        </ul>
                    </div>
                    <div class="layout-sidebar--sidebar-footer">
                        <a class="btn btn-primary">A Button Action </a>
                    </div> 
                </aside>
                <main>
                    <h1>CSS Chassis</h1>
                    <p><span><em>Please note that not all CSS elements are included in the current theme!</em></span></p>
                    <hr />
                    <h1>Heading 1 &lt;h1&gt;</h1>
                    <h2>Heading 2 &lt;h2&gt;</h2>
                    <h3>Heading 3 &lt;h3&gt;</h3>
                    <h4>Heading 4 &lt;h4&gt;</h4>
                    <h5>Heading 5 &lt;h5&gt;</h5>
                    <h6>Heading 6 &lt;h6&gt;</h6>
                    <hr />
                    <h1>Heading 1 &lt;h1&gt;</h1>
                    <p>Many say exploration is part of our destiny, but it’s actually our duty to future generations and their quest to ensure the survival of the human species.</p>
                    <h2>Heading 2 &lt;h2&gt;</h2>
                    <p>It suddenly struck me that that tiny pea, pretty and blue, was the Earth. I put up my thumb and shut one eye, and my thumb blotted out the planet Earth. I didn”t feel like a giant. I felt very, very small.</p>
                    <h3>Heading 3 &lt;h3&gt;</h3>
                    <p>The Earth was small, light blue, and so touchingly alone, our home that must be defended like a holy relic. The Earth was absolutely round. I believe I never knew what the word round meant until I saw Earth from space.</p>
                    <h4>Heading 4 &lt;h4&gt;</h4>
                    <p>To be the first to enter the cosmos, to engage, single-handed, in an unprecedented duel with nature—could one dream of anything more?</p>
                    <h5>Heading 5 &lt;h5&gt;</h5>
                    <p>What was most significant about the lunar voyage was not that man set foot on the Moon but that they set eye on the earth.</p>
                    <h6>Heading 6 &lt;h6&gt;</h6>
                    <p>Many say exploration is part of our destiny, but it’s actually our duty to future generations and their quest to ensure the survival of the human species.</p>
                    <hr />
                    <h2>Block-level elements</h2>
                    <p>Here’s a paragraph &lt;p&gt; filled with some <a href="http://spaceipsum.com/">Space Ipsum</a>. Never in all their history have men been able truly to conceive of the world as one: a single sphere, a globe, having the qualities of a globe, a round earth in which all the directions eventually meet, in which there is no center because every point, or none, is center — an equal earth which all men occupy as equals. The airman’s earth, if free men make it, will be truly round: a globe in practice, not in theory.</p>
                    <p>There can be no thought of finishing for ‘aiming for the stars.’ Both figuratively and literally, it is a task to occupy the generations. And no matter how much progress one makes, there is always the thrill of just beginning.</p>
                    <div>Here’s a div &lt;div&gt;. A Chinese tale tells of some men sent to harm a young girl who, upon seeing her beauty, become her protectors rather than her violators. That’s how I felt seeing the Earth for the first time. I could not help but love and cherish her</div>
                    <article>
                        <h3>This is an article &lt;article&gt; with a paragraph</h3>
                        <p>We want to explore. We’re curious people. Look back over history, people have put their lives at stake to go out and explore … We believe in what we’re doing. Now it’s time to go.</p>
                        <p>End of the article.</p>
                    </article>
                    <blockquote><p>Let’s light this fire one more time, Mike, and witness this great nation at its best. &lt;blockquote&gt; via <a href="http://spaceipsum.com/">Space Ipsum</a></p></blockquote>
                    <p>We choose to go to the moon in this decade and do the other things, not because they are easy, but because they are hard, because that goal will serve to organize and measure the best of our energies and skills, because that challenge is one that we are willing to accept, one we are unwilling to postpone, and one which we intend to win.</p>
                    <pre>*
                        font - family: "Comic Sans MS", "Comic Sans", "Marker Felt" !important;

                        &lt;pre&gt;
                    </pre>
                    <p>Many say exploration is part of our destiny, but it’s actually our duty to future generations and their quest to ensure the survival of the human species.</p>

                  
                </main>
            </section>
        )
    }
}))
